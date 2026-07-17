package com.pasarela.pagos;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Crear cobro con QR end-to-end (HU-008): el comercio verificado obtiene su
 * QR; el pendiente recibe 403; los límites de HU-007 muerden con 422; y la
 * matriz de roles se respeta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrdenesApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbc;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Test
	void unComercioVerificado_creaUnCobroYObtieneElQr() throws Exception {
		ComercioListo comercio = comercioVerificado("890900608-9", "qr@ordenes.co");

		MvcResult respuesta = mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + comercio.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 40000}"))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.referencia").isNotEmpty())
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
				.andExpect(jsonPath("$.monto").value(40000))
				.andExpect(jsonPath("$.expiraEn").isNotEmpty())
				.andExpect(jsonPath("$.qr.contenido").isNotEmpty())
				.andExpect(jsonPath("$.qr.deeplink").isNotEmpty())
				.andReturn();

		String referencia = json.readTree(respuesta.getResponse().getContentAsString())
				.get("referencia").asText();
		assertThat(json.readTree(respuesta.getResponse().getContentAsString())
				.get("qr").get("contenido").asText()).contains(referencia);
	}

	@Test
	void unComercioSinVerificar_noPuedeCobrar_403() throws Exception {
		ComercioListo pendiente = comercioRegistrado("830122566-1", "pendiente@ordenes.co");

		mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + pendiente.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 40000}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value(
						org.hamcrest.Matchers.containsString("verificado")));
	}

	@Test
	void unCobroQueSuperaElTopePorTransaccion_seRechazaCon422_yQuedaEnLaBitacora() throws Exception {
		ComercioListo comercio = comercioVerificado("805000427-1", "tope@ordenes.co");

		mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + comercio.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 2000001}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensaje").value(
						org.hamcrest.Matchers.containsString("tope por transacción")));

		// la auditoría SOBREVIVE al rollback del rechazo (REQUIRES_NEW en la bitácora)
		Integer registros = jdbc.queryForObject(
				"select count(*) from bitacora_operaciones where tipo = 'LIMITE_EXCEDIDO' and actor = ?",
				Integer.class, comercio.id());
		assertThat(registros).isEqualTo(1);
	}

	@Test
	void unMontoInvalido_seRechazaCon400_sinLlegarAlProveedor() throws Exception {
		ComercioListo comercio = comercioVerificado("811007832-5", "monto@ordenes.co");

		mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + comercio.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": -5}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + comercio.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 0}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void elDueno_consultaSuOrdenConTimestampsDeTransicion() throws Exception {
		ComercioListo comercio = comercioVerificado("830037248-0", "detalle@ordenes.co");
		String ordenId = crearOrden(comercio.token(), 35000);

		mvc.perform(get("/api/ordenes/" + ordenId)
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
				.andExpect(jsonPath("$.monto").value(35000))
				.andExpect(jsonPath("$.transiciones[0].desde").value("CREADA"))
				.andExpect(jsonPath("$.transiciones[0].hacia").value("PENDIENTE_PAGO"))
				.andExpect(jsonPath("$.transiciones[0].momento").isNotEmpty())
				// endpoint de polling (ADR-005): jamás servido desde caché
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void otroComercio_recibe404_igualQueUnaOrdenInexistente() throws Exception {
		ComercioListo duenoA = comercioVerificado("890399001-1", "dueno-orden@ordenes.co");
		ComercioListo otroB = comercioVerificado("860034917-5", "otro-orden@ordenes.co");
		String ordenDeA = crearOrden(duenoA.token(), 25000);

		mvc.perform(get("/api/ordenes/" + ordenDeA)
						.header("Authorization", "Bearer " + otroB.token()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/ordenes/" + UUID.randomUUID())
						.header("Authorization", "Bearer " + otroB.token()))
				.andExpect(status().isNotFound());
	}

	@Test
	void laConsultaPublicaPorReferencia_exponeSoloEstadoYMonto() throws Exception {
		ComercioListo comercio = comercioVerificado("900156264-2", "publica@ordenes.co");
		String ordenId = crearOrden(comercio.token(), 15000);
		MvcResult detalle = mvc.perform(get("/api/ordenes/" + ordenId)
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk()).andReturn();
		String referencia = json.readTree(detalle.getResponse().getContentAsString())
				.get("referencia").asText();

		// sin token: es la página de pago del pagador
		MvcResult publica = mvc.perform(get("/api/pagos/" + referencia))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
				.andExpect(jsonPath("$.monto").value(15000))
				// endpoint de polling (ADR-005): jamás servido desde caché
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn();

		// contrato estricto: SOLO estado y monto, nada del comercio ni interno
		assertThat(json.readTree(publica.getResponse().getContentAsString()).fieldNames())
				.toIterable().containsExactlyInAnyOrder("estado", "monto");

		mvc.perform(get("/api/pagos/referencia-inexistente"))
				.andExpect(status().isNotFound());
	}

	@Test
	void elContratoOpenApi_estaPublicadoParaElFrontend() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/ordenes']").exists())
				.andExpect(jsonPath("$.paths['/api/pagos/{referencia}']").exists());
	}

	@Test
	void sinToken_401_yConTokenDeAdmin_403() throws Exception {
		mvc.perform(post("/api/ordenes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 40000}"))
				.andExpect(status().isUnauthorized());

		mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 40000}"))
				.andExpect(status().isForbidden());
	}

	// --- ayudas ---

	private record ComercioListo(String id, String token) {
	}

	private ComercioListo comercioRegistrado(String nit, String email) throws Exception {
		MvcResult registro = mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "razonSocial": "Tienda %s",
								  "nit": "%s",
								  "cuentaLiquidacion": {
								    "banco": "Nequi",
    "tipo": "AHORROS",
								    "numero": "3001234567",
								    "titular": "Tienda %s"
								  },
								  "credenciales": {
								    "email": "%s",
								    "contrasena": "secreta-12345678"
								  }
								}
								""".formatted(nit, nit, nit, email)))
				.andExpect(status().isCreated())
				.andReturn();
		String id = json.readTree(registro.getResponse().getContentAsString())
				.get("id").asText();
		return new ComercioListo(id, token(email, "secreta-12345678"));
	}

	private ComercioListo comercioVerificado(String nit, String email) throws Exception {
		ComercioListo comercio = comercioRegistrado(nit, email);
		mvc.perform(post("/api/comercios/%s/verificacion".formatted(UUID.fromString(comercio.id())))
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk());
		return comercio;
	}

	private String crearOrden(String token, long monto) throws Exception {
		MvcResult creada = mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": %d}".formatted(monto)))
				.andExpect(status().isCreated())
				.andReturn();
		return json.readTree(creada.getResponse().getContentAsString()).get("id").asText();
	}

	private String token(String usuario, String contrasena) throws Exception {
		MvcResult respuesta = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"usuario\": \"%s\", \"contrasena\": \"%s\"}"
								.formatted(usuario, contrasena)))
				.andExpect(status().isOk())
				.andReturn();
		return json.readTree(respuesta.getResponse().getContentAsString())
				.get("token").asText();
	}

}
