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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprobante por transacción end-to-end (HU-020): una orden pagada tiene su
 * soporte completo; una sin pagar responde 422; y el aislamiento entre
 * comercios es el mismo del detalle de la orden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ComprobanteApiTest {

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
	void unaOrdenPagada_tieneComprobanteConLosDatosDelCobro() throws Exception {
		Comercio comercio = comercioVerificado("900650321-2", "comprobante-a@pagos.co");
		Orden orden = crearOrden(comercio.token(), 50000);
		pagarOrden(orden.referencia(), "evt-comp-1", 50000);

		mvc.perform(get("/api/ordenes/%s/comprobante".formatted(orden.id()))
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroComprobante").value(orden.id()))
				.andExpect(jsonPath("$.referencia").value(orden.referencia()))
				.andExpect(jsonPath("$.monto").value(50000))
				.andExpect(jsonPath("$.moneda").value("COP"))
				.andExpect(jsonPath("$.estado").value("PAGO_DETECTADO"))
				.andExpect(jsonPath("$.creadaEn").isNotEmpty())
				.andExpect(jsonPath("$.pagoDetectadoEn").isNotEmpty())
				.andExpect(jsonPath("$.liquidadaEn").value(nullValue()))
				.andExpect(jsonPath("$.emitidoEn").isNotEmpty());

		// el ADMIN puede emitir el comprobante de cualquier comercio (soporte)
		mvc.perform(get("/api/ordenes/%s/comprobante".formatted(orden.id()))
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroComprobante").value(orden.id()));
	}

	@Test
	void unaOrdenSinPagar_noTieneComprobante_422() throws Exception {
		Comercio comercio = comercioVerificado("830512843-0", "comprobante-b@pagos.co");
		Orden pendiente = crearOrden(comercio.token(), 30000);

		mvc.perform(get("/api/ordenes/%s/comprobante".formatted(pendiente.id()))
						.header("Authorization", "Bearer " + comercio.token()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensaje").value(containsString("PENDIENTE_PAGO")));
	}

	@Test
	void unaOrdenAjena_respondeIgualQueUnaInexistente() throws Exception {
		Comercio duenoA = comercioVerificado("901234567-7", "comprobante-c@pagos.co");
		Comercio otroB = comercioVerificado("900377326-9", "comprobante-d@pagos.co");
		Orden ordenDeA = crearOrden(duenoA.token(), 25000);
		pagarOrden(ordenDeA.referencia(), "evt-comp-2", 25000);

		mvc.perform(get("/api/ordenes/%s/comprobante".formatted(ordenDeA.id()))
						.header("Authorization", "Bearer " + otroB.token()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/ordenes/%s/comprobante".formatted(UUID.randomUUID()))
						.header("Authorization", "Bearer " + otroB.token()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/ordenes/%s/comprobante".formatted(ordenDeA.id())))
				.andExpect(status().isUnauthorized());
	}

	// --- ayudas (patrón de VentasApiTest) ---

	private record Comercio(String id, String token) {
	}

	private record Orden(String id, String referencia) {
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

	private Orden crearOrden(String token, long monto) throws Exception {
		MvcResult creada = mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": %d}".formatted(monto)))
				.andExpect(status().isCreated()).andReturn();
		var cuerpo = json.readTree(creada.getResponse().getContentAsString());
		return new Orden(cuerpo.get("id").asText(), cuerpo.get("referencia").asText());
	}

	private void pagarOrden(String referencia, String idEvento, long monto) throws Exception {
		String cuerpo = """
				{"idEvento": "%s", "tipo": "PAGO_RECIBIDO", "referencia": "%s", "monto": %d, "pagadoEn": "2026-07-13T15:05:00Z"}
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
