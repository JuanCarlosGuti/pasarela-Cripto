package com.pasarela.compartido.infraestructura.seguridad;

import com.pasarela.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Headers de seguridad (HU-022): las respuestas de la API — incluidas las de
 * error — salen blindadas contra sniffing de contenido, framing (clickjacking)
 * y cacheo indebido. Vienen de la cadena de Spring Security; este test las
 * congela para que una reconfiguración no las tumbe en silencio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CabecerasDeSeguridadApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void unaRespuestaPublica_llevaLosHeadersDeSeguridad() throws Exception {
		mvc.perform(get("/api/pagos/referencia-inexistente"))
				.andExpect(status().isNotFound())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().exists("Cache-Control"));
	}

	@Test
	void unaRespuestaNoAutenticada_tambienLosLleva() throws Exception {
		mvc.perform(get("/api/ordenes/" + UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"));
	}

}
