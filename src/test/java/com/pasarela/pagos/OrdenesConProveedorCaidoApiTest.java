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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resiliencia del caso de uso ante proveedor caído (T-006 + HU-008): el
 * comercio recibe un error accionable (502) y NO queda ninguna orden
 * fantasma persistida.
 */
@SpringBootTest(properties = "pasarela.proveedores.simulado.modo-fallo=ERROR")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrdenesConProveedorCaidoApiTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${pasarela.seguridad.admin.email}")
	private String adminEmail;

	@Value("${pasarela.seguridad.admin.contrasena}")
	private String adminContrasena;

	@Test
	void conElProveedorCaido_elComercioRecibe502_ySinOrdenesFantasma() throws Exception {
		String comercioId = comercioVerificado("860007738-9", "caido@ordenes.co");
		String token = token("caido@ordenes.co", "secreta-12345678");

		mvc.perform(post("/api/ordenes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"monto\": 40000}"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.mensaje").value(
						org.hamcrest.Matchers.containsString("proveedor")));

		Integer ordenes = jdbc.queryForObject(
				"select count(*) from ordenes where comercio_id = ?::uuid",
				Integer.class, comercioId);
		assertThat(ordenes).isZero();
	}

	// --- ayudas (patrón de OrdenesApiTest) ---

	private String comercioVerificado(String nit, String email) throws Exception {
		MvcResult registro = mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
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
								""".formatted(nit, nit, nit, email)))
				.andExpect(status().isCreated())
				.andReturn();
		String id = json.readTree(registro.getResponse().getContentAsString())
				.get("id").asText();
		mvc.perform(post("/api/comercios/%s/verificacion".formatted(UUID.fromString(id)))
						.header("Authorization", "Bearer " + token(adminEmail, adminContrasena))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk());
		return id;
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
