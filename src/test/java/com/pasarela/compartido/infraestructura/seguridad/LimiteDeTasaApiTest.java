package com.pasarela.compartido.infraestructura.seguridad;

import com.pasarela.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate limiting end-to-end (HU-022): los endpoints públicos responden 429
 * al superar el tope por IP, cada limitador es independiente, y las IPs no
 * se contaminan entre sí. El resto de la suite corre con el límite apagado
 * (application-test) — aquí se enciende con topes chicos a propósito.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"pasarela.seguridad.limite-tasa.habilitado=true",
		"pasarela.seguridad.limite-tasa.ventana-segundos=60",
		"pasarela.seguridad.limite-tasa.login.maximo=3",
		"pasarela.seguridad.limite-tasa.webhook.maximo=2",
		"pasarela.seguridad.limite-tasa.consulta-publica.maximo=2"
})
class LimiteDeTasaApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void elLogin_seBloqueaTrasElTope_yOtraIpConservaSuCupo() throws Exception {
		// 3 intentos fallidos: el limitador los cuenta, la autenticación los rechaza
		for (int i = 0; i < 3; i++) {
			mvc.perform(intentoDeLogin("10.0.0.1")).andExpect(status().isUnauthorized());
		}

		// el cuarto desde la MISMA IP ya ni llega a la autenticación: 429
		mvc.perform(intentoDeLogin("10.0.0.1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "60"))
				.andExpect(jsonPath("$.mensaje").exists());

		// otra IP no paga los platos rotos: fuerza bruta no castiga a vecinos
		mvc.perform(intentoDeLogin("10.0.0.2")).andExpect(status().isUnauthorized());
	}

	@Test
	void elWebhook_seBloqueaTrasElTope_sinTocarLaValidacionDeFirma() throws Exception {
		// sin firma válida responden 401 — pero cuentan para el límite
		for (int i = 0; i < 2; i++) {
			mvc.perform(webhookSinFirma("10.0.1.1")).andExpect(status().isUnauthorized());
		}

		mvc.perform(webhookSinFirma("10.0.1.1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "60"));
	}

	@Test
	void laConsultaPublica_seBloqueaTrasElTope() throws Exception {
		for (int i = 0; i < 2; i++) {
			mvc.perform(consultaPublica("10.0.2.1")).andExpect(status().isNotFound());
		}

		mvc.perform(consultaPublica("10.0.2.1"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void losEndpointsNoPublicos_noEstanLimitados() throws Exception {
		// health se consulta sin tope (monitoreo): 10 > cualquier tope de prueba
		for (int i = 0; i < 10; i++) {
			mvc.perform(get("/actuator/health")
							.with(peticion -> {
								peticion.setRemoteAddr("10.0.3.1");
								return peticion;
							}))
					.andExpect(status().isOk());
		}
	}

	// --- ayudas ---

	private MockHttpServletRequestBuilder intentoDeLogin(String ip) {
		return post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\": \"nadie@nadie.co\", \"contrasena\": \"clave-incorrecta-123\"}")
				.with(peticion -> {
					peticion.setRemoteAddr(ip);
					return peticion;
				});
	}

	private MockHttpServletRequestBuilder webhookSinFirma(String ip) {
		return post("/api/webhooks/simulado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"idEvento\": \"evt-limite\", \"tipo\": \"PAGO_RECIBIDO\"}")
				.with(peticion -> {
					peticion.setRemoteAddr(ip);
					return peticion;
				});
	}

	private MockHttpServletRequestBuilder consultaPublica(String ip) {
		return get("/api/pagos/referencia-que-no-existe")
				.with(peticion -> {
					peticion.setRemoteAddr(ip);
					return peticion;
				});
	}

}
