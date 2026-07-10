package com.pasarela.pagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasarela.TestcontainersConfiguration;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El webhook de confirmación de pago end-to-end (HU-010/011/012): el flujo
 * feliz con el orden exacto de docs/05 y los caminos tristes de docs/07.
 * Escritos ANTES de implementar (retro del Sprint 3): los de HU-012 quedan
 * deshabilitados hasta esa historia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class WebhooksApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private OrdenDePagoRepositorio ordenes;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Value("${pasarela.proveedores.simulado.secreto-webhook}")
	private String secretoWebhook;

	@Test
	void caminoFeliz_elWebhookConfirmaElPago_yQuedaElEventoCrudo() throws Exception {
		Orden orden = ordenPendiente("890903407-9", "feliz@webhooks.co");
		String cuerpo = cuerpoWebhook("evt-feliz-1", orden.referencia(), 40000);

		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("CONFIRMADO"));

		mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("PAGO_DETECTADO"));

		// el evento crudo quedó guardado, con firma válida y procesado
		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where id_externo_evento = 'evt-feliz-1' and firma_valida = true and procesado = true",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void firmaInvalida_responde401_laOrdenNoCambia_yElIntentoQuedaRegistrado() throws Exception {
		Orden orden = ordenPendiente("860005224-6", "firma@webhooks.co");
		String cuerpo = cuerpoWebhook("evt-firma-1", orden.referencia(), 40000);

		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", "firma-falsificada")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isUnauthorized());

		mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));

		// el intento queda registrado con firma_valida=false (auditoría)
		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where proveedor = 'simulado' and firma_valida = false",
				Integer.class)).isGreaterThanOrEqualTo(1);
	}

	@Test
	void eventoDuplicado_responde200_sinDobleConfirmacionNiRegistrosDuplicados() throws Exception {
		Orden orden = ordenPendiente("830095213-0", "duplicado@webhooks.co");
		String cuerpo = cuerpoWebhook("evt-dup-1", orden.referencia(), 40000);

		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("CONFIRMADO"));
		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("DUPLICADO"));

		// una sola fila del evento y una sola transición a PAGO_DETECTADO
		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where id_externo_evento = 'evt-dup-1'",
				Integer.class)).isEqualTo(1);
		MvcResult detalle = mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("PAGO_DETECTADO"))
				.andReturn();
		assertThat(json.readTree(detalle.getResponse().getContentAsString())
				.get("transiciones").size()).isEqualTo(2); // CREADA→PENDIENTE y PENDIENTE→DETECTADO
	}

	@Test
	void ordenInexistente_responde200_yElEventoQuedaParaRevisionConAlertaEnBitacora() throws Exception {
		String cuerpo = cuerpoWebhook("evt-huerfano-1", UUID.randomUUID().toString(), 40000);

		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("PARA_REVISION"));

		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where id_externo_evento = 'evt-huerfano-1' and procesado = false and nota_revision is not null",
				Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject(
				"select count(*) from bitacora_operaciones where tipo = 'WEBHOOK_SIN_ORDEN'",
				Integer.class)).isGreaterThanOrEqualTo(1);
	}

	@Test
	void montoDistinto_laOrdenVaARevision_yElAdminQuedaAlertado() throws Exception {
		Orden orden = ordenPendiente("900373913-4", "monto@webhooks.co");
		String cuerpo = cuerpoWebhook("evt-monto-1", orden.referencia(), 99999);

		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("PARA_REVISION"));

		mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("EN_REVISION"));
		assertThat(jdbc.queryForObject(
				"select count(*) from bitacora_operaciones where tipo = 'MONTO_DISTINTO'",
				Integer.class)).isGreaterThanOrEqualTo(1);
	}

	@Test
	void pagoTardio_sobreOrdenExpirada_noSeConfirma_yQuedaParaRevision() throws Exception {
		Orden orden = ordenPendiente("890905166-8", "tardio@webhooks.co");
		expirarPorDebajoDeLaMesa(orden.id());
		String cuerpo = cuerpoWebhook("evt-tardio-1", orden.referencia(), 40000);

		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("PARA_REVISION"));

		// la orden sigue EXPIRADA: jamás se confirma un pago tardío
		mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("EXPIRADA"));
		// el evento guarda todos los datos para gestionar el reembolso
		assertThat(jdbc.queryForObject(
				"select nota_revision from eventos_proveedor where id_externo_evento = 'evt-tardio-1'",
				String.class)).isNotBlank();
		assertThat(jdbc.queryForObject(
				"select count(*) from bitacora_operaciones where tipo = 'PAGO_TARDIO'",
				Integer.class)).isGreaterThanOrEqualTo(1);
	}

	@Test
	void webhookFueraDeOrden_noCorrompeNada_yElCorrectoAunConfirma() throws Exception {
		Orden orden = ordenPendiente("811026252-4", "fuera@webhooks.co");
		String convertidaAntes = cuerpoWebhook(
				"evt-fuera-1", "PAGO_CONVERTIDO", orden.referencia(), 40000);

		// llega la "conversión" antes que el pago: se tolera sin corromper
		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(convertidaAntes))
						.contentType(MediaType.APPLICATION_JSON).content(convertidaAntes))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("PARA_REVISION"));
		mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));

		// y el evento correcto que llega después confirma con normalidad
		String pagoCorrecto = cuerpoWebhook("evt-fuera-2", orden.referencia(), 40000);
		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(pagoCorrecto))
						.contentType(MediaType.APPLICATION_JSON).content(pagoCorrecto))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("CONFIRMADO"));
		mvc.perform(get("/api/ordenes/" + orden.id())
						.header("Authorization", "Bearer " + orden.token()))
				.andExpect(jsonPath("$.estado").value("PAGO_DETECTADO"));
	}

	// --- ayudas ---

	private record Orden(String id, String referencia, String token) {
	}

	private Orden ordenPendiente(String nit, String email) throws Exception {
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
		String comercioId = json.readTree(registro.getResponse().getContentAsString())
				.get("id").asText();
		mvc.perform(post("/api/comercios/%s/verificacion".formatted(comercioId))
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk());
		String tokenComercio = token(email, "secreta-12345678");
		MvcResult creada = mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + tokenComercio)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 40000}"))
				.andExpect(status().isCreated()).andReturn();
		var cuerpo = json.readTree(creada.getResponse().getContentAsString());
		return new Orden(cuerpo.get("id").asText(), cuerpo.get("referencia").asText(),
				tokenComercio);
	}

	private String cuerpoWebhook(String idEvento, String referencia, long monto) {
		return cuerpoWebhook(idEvento, "PAGO_RECIBIDO", referencia, monto);
	}

	private String cuerpoWebhook(String idEvento, String tipo, String referencia, long monto) {
		return """
				{"idEvento": "%s", "tipo": "%s", "referencia": "%s", "monto": %d, "pagadoEn": "2026-07-09T15:05:00Z"}
				""".formatted(idEvento, tipo, referencia, monto).trim();
	}

	/** Deja la orden EXPIRADA como lo hará el job de HU-014: vía dominio + repositorio. */
	private void expirarPorDebajoDeLaMesa(String ordenId) {
		var orden = ordenes.buscarPorId(
				com.pasarela.pagos.dominio.modelo.IdOrden.de(java.util.UUID.fromString(ordenId)))
				.orElseThrow();
		orden.expirar(orden.expiraEn().plusSeconds(60));
		ordenes.guardar(orden);
	}

	private String firmar(String cuerpo) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secretoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
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
