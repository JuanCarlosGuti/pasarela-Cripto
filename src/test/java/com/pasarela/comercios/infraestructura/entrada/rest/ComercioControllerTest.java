package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.excepcion.ComercioYaRegistradoException;
import com.pasarela.comercios.dominio.excepcion.NitInvalidoException;
import com.pasarela.comercios.dominio.excepcion.VerificacionInvalidaException;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP del controller, sin la cadena de seguridad (addFilters=false):
 * la matriz de permisos completa se prueba end-to-end en SeguridadApiTest.
 */
@WebMvcTest(ComercioController.class)
@AutoConfigureMockMvc(addFilters = false)
class ComercioControllerTest {

	private static final String CUERPO_VALIDO = """
			{
			  "razonSocial": "Tienda La Esquina SAS",
			  "nit": "899999068-1",
			  "cuentaLiquidacion": {
			    "tipo": "NEQUI",
			    "numero": "3001234567",
			    "titular": "Tienda La Esquina SAS"
			  },
			  "credenciales": {
			    "email": "dueno@tienda.co",
			    "contrasena": "secreta123"
			  }
			}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarComercioUseCase registrarComercio;

	@MockitoBean
	private DecidirVerificacionUseCase decidirVerificacion;

	@MockitoBean
	private ConsultarComercioUseCase consultarComercio;

	@Test
	void post_conDatosValidos_responde201ConUbicacionYEstadoPendiente() throws Exception {
		when(registrarComercio.registrar(any())).thenReturn(comercioRegistrado());

		mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(CUERPO_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.razonSocial").value("Tienda La Esquina SAS"))
				.andExpect(jsonPath("$.nit").value("899999068-1"))
				.andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"))
				// el contrato no expone la cuenta de liquidación (dato sensible)
				.andExpect(jsonPath("$.cuentaLiquidacion").doesNotExist());
	}

	@Test
	void post_conNitInvalido_responde400ConMensajeClaro() throws Exception {
		when(registrarComercio.registrar(any()))
				.thenThrow(new NitInvalidoException(
						"El dígito de verificación del NIT 899999068 no corresponde (se esperaba 1)"));

		mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(CUERPO_VALIDO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(
						org.hamcrest.Matchers.containsString("dígito de verificación")));
	}

	@Test
	void post_conNitDuplicado_responde409() throws Exception {
		when(registrarComercio.registrar(any()))
				.thenThrow(new ComercioYaRegistradoException(
						"Ya existe un comercio registrado con el NIT 899999068-1"));

		mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(CUERPO_VALIDO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").isNotEmpty());
	}

	@Test
	void post_sinRazonSocial_responde400_sinLlegarAlCasoDeUso() throws Exception {
		String sinRazonSocial = """
				{
				  "nit": "899999068-1",
				  "cuentaLiquidacion": {
				    "tipo": "NEQUI",
				    "numero": "3001234567",
				    "titular": "Tienda"
				  },
				  "credenciales": {
				    "email": "dueno@tienda.co",
				    "contrasena": "secreta123"
				  }
				}
				""";

		mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(sinRazonSocial))
				.andExpect(status().isBadRequest());
	}

	@Test
	void postVerificacion_aprobar_responde200ConElComercioVerificado() throws Exception {
		Comercio verificado = comercioRegistrado();
		verificado.verificar(Instant.parse("2026-07-08T15:00:00Z"));
		when(decidirVerificacion.decidir(any())).thenReturn(verificado);

		mvc.perform(post("/api/comercios/{id}/verificacion", verificado.id().valor())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADO"))
				// el motivo y la cuenta no se exponen en el contrato público
				.andExpect(jsonPath("$.motivoDecision").doesNotExist())
				.andExpect(jsonPath("$.cuentaLiquidacion").doesNotExist());
	}

	@Test
	void postVerificacion_sobreComercioInexistente_responde404() throws Exception {
		when(decidirVerificacion.decidir(any()))
				.thenThrow(new ComercioNoEncontradoException("No existe un comercio con ese id"));

		mvc.perform(post("/api/comercios/{id}/verificacion",
						"3426c255-8bea-454b-9e1b-9aa923d8af98")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"APROBAR\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void postVerificacion_conTransicionInvalida_responde409() throws Exception {
		when(decidirVerificacion.decidir(any()))
				.thenThrow(new VerificacionInvalidaException(
						"No se puede suspender un comercio en estado PENDIENTE (se requiere VERIFICADO)"));

		mvc.perform(post("/api/comercios/{id}/verificacion",
						"3426c255-8bea-454b-9e1b-9aa923d8af98")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"decision\": \"SUSPENDER\", \"motivo\": \"actividad inusual\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").isNotEmpty());
	}

	@Test
	void postVerificacion_sinDecision_responde400_sinLlegarAlCasoDeUso() throws Exception {
		mvc.perform(post("/api/comercios/{id}/verificacion",
						"3426c255-8bea-454b-9e1b-9aa923d8af98")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"motivo\": \"sin decisión\"}"))
				.andExpect(status().isBadRequest());
	}

	private static Comercio comercioRegistrado() {
		return Comercio.registrar(
				"Tienda La Esquina SAS",
				Nit.de("899999068-1"),
				new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", "Tienda La Esquina SAS"),
				Instant.parse("2026-07-08T10:00:00Z"));
	}

}