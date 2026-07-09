package com.pasarela.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasarela.TestcontainersConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de permisos de HU-006, end-to-end contra la cadena de seguridad
 * real y PostgreSQL (Testcontainers): 401 sin revelar existencia, tokens
 * expirados/manipulados, 403 por rol y aislamiento entre comercios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SeguridadApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private JwtEncoder emisorDeJwt;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Nested
	class Login {

		@Test
		void loginAdmin_devuelveTokenConExpiracionYRol() throws Exception {
			MvcResult respuesta = mvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(cuerpoLogin(adminEmail, adminContrasena)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.token").isNotEmpty())
					.andExpect(jsonPath("$.expiraEn").isNotEmpty())
					.andExpect(jsonPath("$.rol").value("ADMIN"))
					.andReturn();

			assertThat(respuesta.getResponse().getContentAsString())
					.doesNotContain(adminContrasena);
		}

		@Test
		void login_conContrasenaMala_yConUsuarioInexistente_respondenIdentico() throws Exception {
			String conContrasenaMala = mvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(cuerpoLogin(adminEmail, "contrasena-incorrecta")))
					.andExpect(status().isUnauthorized())
					.andReturn().getResponse().getContentAsString();
			String conUsuarioInexistente = mvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(cuerpoLogin("nadie@ninguna.parte", "loquesea")))
					.andExpect(status().isUnauthorized())
					.andReturn().getResponse().getContentAsString();

			// mismo cuerpo exacto: no se revela si la cuenta existe
			assertThat(conContrasenaMala).isEqualTo(conUsuarioInexistente);
		}
	}

	@Nested
	class ProteccionDeEndpoints {

		@Test
		void endpointProtegido_sinToken_responde401() throws Exception {
			mvc.perform(post("/api/comercios/%s/verificacion".formatted(UUID.randomUUID()))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"decision\": \"APROBAR\"}"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void endpointProtegido_conTokenManipulado_responde401() throws Exception {
			String tokenManipulado = tokenDeAdmin() + "x";

			mvc.perform(get("/api/comercios/" + UUID.randomUUID())
							.header("Authorization", "Bearer " + tokenManipulado))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void endpointProtegido_conTokenExpirado_responde401() throws Exception {
			// emitido con el secreto real pero vencido hace una hora
			Instant haceDosHoras = Instant.now().minusSeconds(7200);
			JwtClaimsSet claims = JwtClaimsSet.builder()
					.subject(adminEmail)
					.issuedAt(haceDosHoras)
					.expiresAt(haceDosHoras.plusSeconds(3600))
					.claim("rol", "ADMIN")
					.build();
			String tokenExpirado = emisorDeJwt.encode(JwtEncoderParameters.from(
					JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

			mvc.perform(get("/api/comercios/" + UUID.randomUUID())
							.header("Authorization", "Bearer " + tokenExpirado))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void verificacion_conRolComercio_responde403() throws Exception {
			String tokenComercio = registrarYLoguearComercio(
					"800197268-4", "rol403@test.co").token();

			mvc.perform(post("/api/comercios/%s/verificacion".formatted(UUID.randomUUID()))
							.header("Authorization", "Bearer " + tokenComercio)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"decision\": \"APROBAR\"}"))
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	class FlujoCompletoYAislamiento {

		@Test
		void registro_loginAdmin_verificacion_loginComercio_yAislamiento() throws Exception {
			// el criterio de cierre del Sprint 2, de principio a fin por HTTP
			ComercioAutenticado comercioA = registrarYLoguearComercio(
					"899999068-1", "dueno-a@test.co");
			ComercioAutenticado comercioB = registrarYLoguearComercio(
					"890903938-8", "dueno-b@test.co");

			// el admin aprueba al comercio A
			mvc.perform(post("/api/comercios/%s/verificacion".formatted(comercioA.id()))
							.header("Authorization", "Bearer " + tokenDeAdmin())
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"decision\": \"APROBAR\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADO"));

			// cada comercio ve el suyo
			mvc.perform(get("/api/comercios/" + comercioA.id())
							.header("Authorization", "Bearer " + comercioA.token()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADO"));

			// aislamiento: B pidiendo el comercio de A recibe 404 (no 403:
			// la existencia no se filtra), igual que un id inventado
			mvc.perform(get("/api/comercios/" + comercioA.id())
							.header("Authorization", "Bearer " + comercioB.token()))
					.andExpect(status().isNotFound());
			mvc.perform(get("/api/comercios/" + UUID.randomUUID())
							.header("Authorization", "Bearer " + comercioB.token()))
					.andExpect(status().isNotFound());

			// el admin sí puede ver cualquiera
			mvc.perform(get("/api/comercios/" + comercioB.id())
							.header("Authorization", "Bearer " + tokenDeAdmin()))
					.andExpect(status().isOk());
		}

		@Test
		void limites_soloElAdminPuedeCambiarlos_yQuedanEnLaRespuesta() throws Exception {
			ComercioAutenticado comercio = registrarYLoguearComercio(
					"860002964-4", "limites@test.co");
			String cuerpoLimites = """
					{"topePorTransaccion": 5000000, "topeMensual": 50000000}
					""";

			// un COMERCIO no puede tocar límites (ni siquiera los suyos)
			mvc.perform(put("/api/comercios/%s/limites".formatted(comercio.id()))
							.header("Authorization", "Bearer " + comercio.token())
							.contentType(MediaType.APPLICATION_JSON)
							.content(cuerpoLimites))
					.andExpect(status().isForbidden());

			// el ADMIN sí; la respuesta refleja los topes nuevos
			mvc.perform(put("/api/comercios/%s/limites".formatted(comercio.id()))
							.header("Authorization", "Bearer " + tokenDeAdmin())
							.contentType(MediaType.APPLICATION_JSON)
							.content(cuerpoLimites))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.limites.topePorTransaccion").value(5000000))
					.andExpect(jsonPath("$.limites.topeMensual").value(50000000));
		}

		@Test
		void registro_conEmailDeAccesoYaUsado_responde409() throws Exception {
			registrarYLoguearComercio("811021363-0", "repetido@test.co");

			mvc.perform(post("/api/comercios")
							.contentType(MediaType.APPLICATION_JSON)
							.content(cuerpoRegistro("830053800-4", "repetido@test.co")))
					.andExpect(status().isConflict());
		}
	}

	// --- ayudas ---

	private record ComercioAutenticado(String id, String token) {
	}

	private String tokenDeAdmin() throws Exception {
		return token(adminEmail, adminContrasena);
	}

	private String token(String usuario, String contrasena) throws Exception {
		MvcResult respuesta = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoLogin(usuario, contrasena)))
				.andExpect(status().isOk())
				.andReturn();
		return json.readTree(respuesta.getResponse().getContentAsString())
				.get("token").asText();
	}

	private ComercioAutenticado registrarYLoguearComercio(String nit, String email)
			throws Exception {
		MvcResult registro = mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoRegistro(nit, email)))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode cuerpo = json.readTree(registro.getResponse().getContentAsString());
		return new ComercioAutenticado(
				cuerpo.get("id").asText(), token(email, "secreta-12345678"));
	}

	private static String cuerpoLogin(String usuario, String contrasena) {
		return """
				{"usuario": "%s", "contrasena": "%s"}
				""".formatted(usuario, contrasena);
	}

	private static String cuerpoRegistro(String nit, String email) {
		return """
				{
				  "razonSocial": "Tienda %s",
				  "nit": "%s",
				  "cuentaLiquidacion": {
				    "tipo": "NEQUI",
				    "numero": "3001234567",
				    "titular": "Tienda %s"
				  },
				  "credenciales": {
				    "email": "%s",
				    "contrasena": "secreta-12345678"
				  }
				}
				""".formatted(nit, nit, nit, email);
	}

}
