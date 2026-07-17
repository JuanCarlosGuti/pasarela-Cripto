package com.pasarela.liquidaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasarela.TestcontainersConfiguration;
import com.pasarela.compartido.dominio.modelo.IdOrden;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registro de liquidación end-to-end (HU-016): agrupa órdenes CONVERTIDA,
 * el dinero cuadra al centavo, las órdenes pasan a LIQUIDADA y ninguna
 * puede pertenecer a dos liquidaciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class LiquidacionesApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private OrdenDePagoRepositorio ordenes;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Test
	void elAdminRegistraUnaLiquidacion_yElDineroCuadraAlCentavo() throws Exception {
		Comercio comercio = comercioVerificado("890926617-8", "liq@liquidaciones.co");
		// tres cobros de 33.333 pagados y convertidos: fuerzan el redondeo
		String orden1 = ordenConvertida(comercio, 33333, "evt-liq-1");
		String orden2 = ordenConvertida(comercio, 33333, "evt-liq-2");
		String orden3 = ordenConvertida(comercio, 33333, "evt-liq-3");

		MvcResult respuesta = mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s", "%s", "%s"]}
								""".formatted(comercio.id(), orden1, orden2, orden3)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.montoBruto").value(99999))
				.andExpect(jsonPath("$.comisionPlataforma").value(2500))
				// rampa simulada (HU-025): 0.8% de 99.999 = 799,992 → redondeo a 800
				.andExpect(jsonPath("$.comisionRampa").value(800))
				.andExpect(jsonPath("$.tasaCambioSimulada").value(4150))
				.andExpect(jsonPath("$.cuentaDestinoDescripcion").value(
						org.hamcrest.Matchers.containsString("NEQUI")))
				.andExpect(jsonPath("$.referenciaProveedor").value(
						org.hamcrest.Matchers.startsWith("RAMPA-SIM-")))
				.andExpect(jsonPath("$.montoNetoComercio").value(96699))
				.andExpect(jsonPath("$.estado").value("REGISTRADA"))
				.andReturn();

		// comisión plataforma + comisión rampa + neto = bruto, al centavo
		var cuerpo = json.readTree(respuesta.getResponse().getContentAsString());
		assertThat(cuerpo.get("comisionPlataforma").asLong()
				+ cuerpo.get("comisionRampa").asLong()
				+ cuerpo.get("montoNetoComercio").asLong())
				.isEqualTo(cuerpo.get("montoBruto").asLong());

		// las órdenes quedaron LIQUIDADA
		mvc.perform(get("/api/ordenes/" + orden1)
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(jsonPath("$.estado").value("LIQUIDADA"));

		// una orden no puede pertenecer a DOS liquidaciones
		mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s"]}
								""".formatted(comercio.id(), orden1)))
				.andExpect(status().isConflict());
	}

	@Test
	void elComercio_veSusPropiasLiquidaciones_conElDesgloseDeLaRampa() throws Exception {
		Comercio comercio = comercioVerificado("900373115-3", "vetodo@liquidaciones.co");
		Comercio otro = comercioVerificado("901313864-9", "otro@liquidaciones.co");
		String orden1 = ordenConvertida(comercio, 25000, "evt-get-1");

		mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s"]}
								""".formatted(comercio.id(), orden1)))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/liquidaciones")
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].montoBruto").value(25000))
				.andExpect(jsonPath("$[0].comisionRampa").value(200));

		// aislamiento: otro comercio no ve liquidaciones ajenas
		mvc.perform(get("/api/liquidaciones")
						.header("Authorization", "Bearer " + otro.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

		// el admin no consulta por esta vía (es del comercio, sale del token)
		mvc.perform(get("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin()))
				.andExpect(status().isForbidden());
	}

	@Test
	void laConciliacion_pasaAConciliadaSiCoincide_yADiscrepanciaConAlertaSiNo() throws Exception {
		Comercio comercio = comercioVerificado("890300279-4", "conc@liquidaciones.co");
		String orden1 = ordenConvertida(comercio, 40000, "evt-conc-1");
		String orden2 = ordenConvertida(comercio, 60000, "evt-conc-2");

		MvcResult creada = mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s", "%s"]}
								""".formatted(comercio.id(), orden1, orden2)))
				.andExpect(status().isCreated()).andReturn();
		String liq1 = json.readTree(creada.getResponse().getContentAsString())
				.get("id").asText();

		// el proveedor reporta EXACTAMENTE lo registrado → CONCILIADA
		mvc.perform(post("/api/liquidaciones/%s/conciliacion".formatted(liq1))
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"montoBruto": 100000, "ordenes": ["%s", "%s"]}
								""".formatted(orden1, orden2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CONCILIADA"))
				.andExpect(jsonPath("$.detalleDiscrepancia").value(org.hamcrest.Matchers.nullValue()));

		// re-conciliar una CONCILIADA → 409: la decisión no se pisa
		mvc.perform(post("/api/liquidaciones/%s/conciliacion".formatted(liq1))
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"montoBruto": 100000, "ordenes": ["%s", "%s"]}
								""".formatted(orden1, orden2)))
				.andExpect(status().isConflict());

		// segunda liquidación con reporte que NO cuadra → DISCREPANCIA + alerta
		String orden3 = ordenConvertida(comercio, 50000, "evt-conc-3");
		MvcResult creada2 = mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s"]}
								""".formatted(comercio.id(), orden3)))
				.andExpect(status().isCreated()).andReturn();
		String liq2 = json.readTree(creada2.getResponse().getContentAsString())
				.get("id").asText();

		mvc.perform(post("/api/liquidaciones/%s/conciliacion".formatted(liq2))
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"montoBruto": 49000, "ordenes": ["%s"]}
								""".formatted(orden3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("DISCREPANCIA"))
				.andExpect(jsonPath("$.detalleDiscrepancia").value(
						org.hamcrest.Matchers.containsString("49000")));

		assertThat(jdbc.queryForObject(
				"select count(*) from bitacora_operaciones where tipo = 'DISCREPANCIA_CONCILIACION'",
				Integer.class)).isGreaterThanOrEqualTo(1);
	}

	@Test
	void unaOrdenSinConvertir_noEsLiquidable_422() throws Exception {
		Comercio comercio = comercioVerificado("860531287-5", "sinconv@liquidaciones.co");
		String pendiente = crearOrden(comercio.token(), 40000); // sigue PENDIENTE_PAGO

		mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s"]}
								""".formatted(comercio.id(), pendiente)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensaje").value(
						org.hamcrest.Matchers.containsString("CONVERTIDA")));
	}

	@Test
	void soloElAdmin_puedeRegistrarLiquidaciones() throws Exception {
		Comercio comercio = comercioVerificado("830008686-1", "rol@liquidaciones.co");

		mvc.perform(post("/api/liquidaciones")
						.header("Authorization", "Bearer " + comercio.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"comercioId": "%s", "ordenes": ["%s"]}
								""".formatted(comercio.id(), UUID.randomUUID())))
				.andExpect(status().isForbidden());
	}

	@Test
	void laConstraintDeOrdenUnica_muerdeANivelSql() {
		UUID ordenId = UUID.randomUUID();
		UUID liq1 = UUID.randomUUID();
		UUID liq2 = UUID.randomUUID();
		jdbc.update("insert into liquidaciones (id, comercio_id, monto_bruto, comision_plataforma, monto_neto_comercio, referencia_proveedor, estado, liquidada_en) values (?, ?, 100, 2, 98, 'a', 'REGISTRADA', now())", liq1, UUID.randomUUID());
		jdbc.update("insert into liquidaciones (id, comercio_id, monto_bruto, comision_plataforma, monto_neto_comercio, referencia_proveedor, estado, liquidada_en) values (?, ?, 100, 2, 98, 'b', 'REGISTRADA', now())", liq2, UUID.randomUUID());
		jdbc.update("insert into liquidacion_ordenes (liquidacion_id, orden_id) values (?, ?)", liq1, ordenId);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
				"insert into liquidacion_ordenes (liquidacion_id, orden_id) values (?, ?)", liq2, ordenId))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	// --- ayudas ---

	private record Comercio(String id, String token) {
	}

	/** Crea, paga (webhook real) y convierte (vía repositorio) una orden. */
	private String ordenConvertida(Comercio comercio, long monto, String idEvento)
			throws Exception {
		String ordenId = crearOrden(comercio.token(), monto);
		MvcResult detalle = mvc.perform(get("/api/ordenes/" + ordenId)
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk()).andReturn();
		String referencia = json.readTree(detalle.getResponse().getContentAsString())
				.get("referencia").asText();
		String cuerpo = """
				{"idEvento": "%s", "tipo": "PAGO_RECIBIDO", "referencia": "%s", "monto": %d, "pagadoEn": "%s"}
				""".formatted(idEvento, referencia, monto, Instant.now()).trim();
		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(jsonPath("$.resultado").value("CONFIRMADO"));
		// la conversión llega con el proveedor real; aquí se simula vía dominio
		var orden = ordenes.buscarPorId(IdOrden.de(UUID.fromString(ordenId))).orElseThrow();
		orden.marcarComoConvertida(Instant.now());
		ordenes.guardar(orden);
		return ordenId;
	}

	private String crearOrden(String token, long monto) throws Exception {
		MvcResult creada = mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": %d}".formatted(monto)))
				.andExpect(status().isCreated()).andReturn();
		return json.readTree(creada.getResponse().getContentAsString()).get("id").asText();
	}

	private Comercio comercioVerificado(String nit, String email) throws Exception {
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
		String id = json.readTree(registro.getResponse().getContentAsString())
				.get("id").asText();
		mvc.perform(post("/api/comercios/%s/verificacion".formatted(id))
						.header("Authorization", "Bearer " + tokenDeAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk());
		return new Comercio(id, token(email, "secreta-12345678"));
	}

	private String tokenDeAdmin() throws Exception {
		return token(adminEmail, adminContrasena);
	}

	@Value("${pasarela.proveedores.simulado.secreto-webhook}")
	private String secretoWebhook;

	private String firmar(String cuerpo) throws Exception {
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
		mac.init(new javax.crypto.spec.SecretKeySpec(
				secretoWebhook.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
		return java.util.HexFormat.of().formatHex(
				mac.doFinal(cuerpo.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
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
