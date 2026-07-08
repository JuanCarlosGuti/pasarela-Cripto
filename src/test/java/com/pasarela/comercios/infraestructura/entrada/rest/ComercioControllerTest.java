package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.excepcion.ComercioYaRegistradoException;
import com.pasarela.comercios.dominio.excepcion.NitInvalidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(ComercioController.class)
class ComercioControllerTest {

	private static final String CUERPO_VALIDO = """
			{
			  "razonSocial": "Tienda La Esquina SAS",
			  "nit": "899999068-1",
			  "cuentaLiquidacion": {
			    "tipo": "NEQUI",
			    "numero": "3001234567",
			    "titular": "Tienda La Esquina SAS"
			  }
			}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarComercioUseCase registrarComercio;

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
				  }
				}
				""";

		mvc.perform(post("/api/comercios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(sinRazonSocial))
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