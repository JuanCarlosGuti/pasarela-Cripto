package com.pasarela.comercios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasarela.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cola de verificación del Admin (HU-026): listar comercios, filtrar por
 * estado, y la matriz de acceso (solo ADMIN ve la colección).
 *
 * <p>La BD se comparte entre clases de test (mismo contexto Spring), así que
 * las aserciones son "contiene los míos", nunca conteos globales.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ComerciosAdminApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Test
	void elAdminListaLosComercios_yPuedeFiltrarPorEstado() throws Exception {
		// NITs propios de esta clase: la BD se comparte con las demás suites
		// (mismo contexto Spring) y repetir un NIT rompe SUS registros con 409
		String pendiente = registrar("902110045-5", "pend@admin-lista.co");
		String verificado = registrar("902110046-2", "verif@admin-lista.co");
		aprobar(verificado);

		// sin filtro: aparecen ambos
		List<String> todos = idsDe(listar(""));
		assertThat(todos).contains(pendiente, verificado);

		// con filtro PENDIENTE: el pendiente sí, el verificado no, y TODO lo
		// devuelto viene en ese estado
		JsonNode pendientes = listar("?estado=PENDIENTE");
		assertThat(idsDe(pendientes)).contains(pendiente).doesNotContain(verificado);
		pendientes.forEach(comercio ->
				assertThat(comercio.get("estadoVerificacion").asText()).isEqualTo("PENDIENTE"));
	}

	@Test
	void unEstadoInexistente_responde400ConMensaje() throws Exception {
		mvc.perform(get("/api/comercios?estado=INVENTADO")
						.header("Authorization", "Bearer " + tokenDeAdmin()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(
						org.hamcrest.Matchers.containsString("INVENTADO")));
	}

	@Test
	void unComercio_noPuedeVerLaColeccion_403() throws Exception {
		registrar("902110047-1", "rol@admin-lista.co");
		String tokenComercio = token("rol@admin-lista.co", "secreta-12345678");

		mvc.perform(get("/api/comercios")
						.header("Authorization", "Bearer " + tokenComercio))
				.andExpect(status().isForbidden());
	}

	@Test
	void sinToken_laColeccionResponde401() throws Exception {
		mvc.perform(get("/api/comercios")).andExpect(status().isUnauthorized());
	}

	// --- ayudas ---

	private JsonNode listar(String filtro) throws Exception {
		MvcResult respuesta = mvc.perform(get("/api/comercios" + filtro)
						.header("Authorization", "Bearer " + tokenDeAdmin()))
				.andExpect(status().isOk()).andReturn();
		return json.readTree(respuesta.getResponse().getContentAsString());
	}

	private static List<String> idsDe(JsonNode lista) {
		List<String> ids = new ArrayList<>();
		lista.forEach(comercio -> ids.add(comercio.get("id").asText()));
		return ids;
	}

	private String registrar(String nit, String email) throws Exception {
		MvcResult registro = mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "razonSocial": "Tienda %s",
								  "nit": "%s",
								  "cuentaLiquidacion": {"tipo": "NEQUI", "numero": "3001234567", "titular": "Tienda %s"},
								  "credenciales": {"email": "%s", "contrasena": "secreta-12345678"}
								}
								""".formatted(nit, nit, nit, email)))
				.andExpect(status().isCreated()).andReturn();
		return json.readTree(registro.getResponse().getContentAsString()).get("id").asText();
	}

	private void aprobar(String comercioId) throws Exception {
		mvc.perform(post("/api/comercios/%s/verificacion".formatted(comercioId))
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk());
	}

	private String tokenDeAdmin() throws Exception {
		return token(adminEmail, adminContrasena);
	}

	private String token(String usuario, String contrasena) throws Exception {
		MvcResult respuesta = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"usuario\": \"%s\", \"contrasena\": \"%s\"}"
								.formatted(usuario, contrasena)))
				.andExpect(status().isOk()).andReturn();
		return json.readTree(respuesta.getResponse().getContentAsString())
				.get("token").asText();
	}

}
