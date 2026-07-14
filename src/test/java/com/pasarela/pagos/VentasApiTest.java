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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dashboard de ventas end-to-end (HU-018/019): los totales solo cuentan ventas
 * efectivas, el listado pagina, el export es CSV contador-ready, y cada
 * comercio ve EXCLUSIVAMENTE lo suyo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class VentasApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Value("${pasarela.proveedores.simulado.secreto-webhook}")
	private String secretoWebhook;

	@Test
	void elResumen_soloCuentaVentasEfectivas_yCadaComercioVeLoSuyo() throws Exception {
		Comercio comercioA = comercioVerificado("890917517-1", "ventas-a@dashboard.co");
		Comercio comercioB = comercioVerificado("860007336-1", "ventas-b@dashboard.co");
		// A: dos ventas efectivas (pagadas por webhook) y una que NO cuenta (pendiente)
		pagarOrden(crearOrden(comercioA.token(), 40000), "evt-va-1", 40000);
		pagarOrden(crearOrden(comercioA.token(), 35000), "evt-va-2", 35000);
		crearOrden(comercioA.token(), 99000); // PENDIENTE_PAGO: no suma
		// B: una venta efectiva propia
		pagarOrden(crearOrden(comercioB.token(), 12000), "evt-vb-1", 12000);

		// A ve SOLO sus dos ventas efectivas — ni la pendiente ni las de B
		mvc.perform(get("/api/ventas/resumen")
						.header("Authorization", "Bearer " + comercioA.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dia.total").value(75000))
				.andExpect(jsonPath("$.dia.cantidad").value(2))
				.andExpect(jsonPath("$.mes.total").value(75000))
				.andExpect(jsonPath("$.mes.cantidad").value(2));

		// B ve solo lo suyo (aislamiento estructural: el comercio sale del token)
		mvc.perform(get("/api/ventas/resumen")
						.header("Authorization", "Bearer " + comercioB.token()))
				.andExpect(jsonPath("$.dia.total").value(12000))
				.andExpect(jsonPath("$.dia.cantidad").value(1));

		// el listado incluye TODOS los estados de A (3 órdenes) y pagina
		mvc.perform(get("/api/ventas?pagina=0&tamano=2")
						.header("Authorization", "Bearer " + comercioA.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(3))
				.andExpect(jsonPath("$.ordenes.length()").value(2));
		mvc.perform(get("/api/ventas?pagina=1&tamano=2")
						.header("Authorization", "Bearer " + comercioA.token()))
				.andExpect(jsonPath("$.ordenes.length()").value(1));

		// tamaño de página fuera de rango → 400
		mvc.perform(get("/api/ventas?tamano=500")
						.header("Authorization", "Bearer " + comercioA.token()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sinToken_401_yConTokenDeAdmin_403() throws Exception {
		mvc.perform(get("/api/ventas/resumen"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/ventas/resumen")
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena)))
				.andExpect(status().isForbidden());
	}

	@Test
	void exportar_devuelveCsvConBomYSepararorPuntoYComa_conLasFilasDelComercio() throws Exception {
		Comercio comercio = comercioVerificado("890925368-4", "export-a@dashboard.co");
		pagarOrden(crearOrden(comercio.token(), 40000), "evt-exp-1", 40000);
		crearOrden(comercio.token(), 15000); // PENDIENTE_PAGO: el export SÍ la incluye

		MvcResult respuesta = mvc.perform(get("/api/ventas/exportar")
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
				.andExpect(header().exists("Content-Disposition"))
				.andReturn();

		byte[] cuerpo = respuesta.getResponse().getContentAsByteArray();
		// BOM UTF-8: EF BB BF, exigido por el criterio (Excel en español)
		assertThat(cuerpo[0] & 0xFF).isEqualTo(0xEF);
		assertThat(cuerpo[1] & 0xFF).isEqualTo(0xBB);
		assertThat(cuerpo[2] & 0xFF).isEqualTo(0xBF);

		String csv = new String(cuerpo, java.nio.charset.StandardCharsets.UTF_8);
		String[] lineas = csv.replace("﻿", "").split("\r\n");
		assertThat(lineas[0]).isEqualTo("Fecha;Referencia;Monto bruto;Comisión;Neto;Estado");
		// 2 filas de datos: la pagada (2.5% de 40000=1000, neto 39000) y la pendiente
		assertThat(lineas).hasSize(3);
		assertThat(csv).contains(";40000;1000;39000;PAGO_DETECTADO")
				.contains(";15000;375;14625;PENDIENTE_PAGO");
	}

	@Test
	void exportar_conRangoSinMovimientos_devuelveArchivoValidoSoloConEncabezados() throws Exception {
		Comercio comercio = comercioVerificado("900552080-2", "export-vacio@dashboard.co");

		MvcResult respuesta = mvc.perform(get("/api/ventas/exportar")
						.param("desde", "2020-01-01")
						.param("hasta", "2020-01-31")
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk())
				.andReturn();

		String csv = new String(respuesta.getResponse().getContentAsByteArray(),
				java.nio.charset.StandardCharsets.UTF_8).replace("﻿", "");
		assertThat(csv.trim()).isEqualTo("Fecha;Referencia;Monto bruto;Comisión;Neto;Estado");
	}

	@Test
	void exportar_aislaLosMovimientosPorComercio() throws Exception {
		Comercio comercioA = comercioVerificado("900123765-9", "export-b-a@dashboard.co");
		Comercio comercioB = comercioVerificado("811039091-1", "export-b-b@dashboard.co");
		crearOrden(comercioA.token(), 77000);
		crearOrden(comercioB.token(), 88000);

		MvcResult respuestaB = mvc.perform(get("/api/ventas/exportar")
						.header("Authorization", "Bearer " + comercioB.token()))
				.andExpect(status().isOk())
				.andReturn();

		String csvB = new String(respuestaB.getResponse().getContentAsByteArray(),
				java.nio.charset.StandardCharsets.UTF_8);
		assertThat(csvB).contains("88000").doesNotContain("77000");
	}

	// --- ayudas (patrón de WebhooksApiTest) ---

	private record Comercio(String id, String token) {
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
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk());
		return new Comercio(id, token(email, "secreta-12345678"));
	}

	private String crearOrden(String token, long monto) throws Exception {
		MvcResult creada = mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": %d}".formatted(monto)))
				.andExpect(status().isCreated()).andReturn();
		return json.readTree(creada.getResponse().getContentAsString())
				.get("referencia").asText();
	}

	private void pagarOrden(String referencia, String idEvento, long monto) throws Exception {
		String cuerpo = """
				{"idEvento": "%s", "tipo": "PAGO_RECIBIDO", "referencia": "%s", "monto": %d, "pagadoEn": "2026-07-10T15:05:00Z"}
				""".formatted(idEvento, referencia, monto).trim();
		mvc.perform(post("/api/webhooks/simulado")
						.header("X-Firma", firmar(cuerpo))
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("CONFIRMADO"));
	}

	private String firmar(String cuerpo) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(
				secretoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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
