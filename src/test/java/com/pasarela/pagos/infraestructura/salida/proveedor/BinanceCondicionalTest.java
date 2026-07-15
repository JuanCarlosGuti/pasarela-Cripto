package com.pasarela.pagos.infraestructura.salida.proveedor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.security.KeyPairGenerator;
import java.time.Clock;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-021: el adaptador real solo existe si la propiedad lo habilita — el
 * switch simulador↔Binance es SOLO configuración (ADR-003). Con ambos
 * deshabilitados no hay adaptador del puerto y el arranque falla rápido.
 */
class BinanceCondicionalTest {

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(ConfiguracionDePrueba.class);

	@Test
	void sinLaPropiedad_elBeanNoExiste() {
		contexto.run(ctx -> assertThat(ctx).doesNotHaveBean(BinancePayAdapter.class));
	}

	@Test
	void conLaPropiedadEnFalse_elBeanTampocoExiste() {
		contexto.withPropertyValues("pasarela.proveedores.binance.habilitado=false")
				.run(ctx -> assertThat(ctx).doesNotHaveBean(BinancePayAdapter.class));
	}

	@Test
	void habilitadoYConCredenciales_elBeanExiste() throws Exception {
		KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		String clavePublica = Base64.getEncoder()
				.encodeToString(generador.generateKeyPair().getPublic().getEncoded());

		contexto.withPropertyValues(
						"pasarela.proveedores.binance.habilitado=true",
						"pasarela.proveedores.binance.base-url=http://localhost:1",
						"pasarela.proveedores.binance.api-key=api-key-de-prueba",
						"pasarela.proveedores.binance.secreto=secreto-de-prueba",
						"pasarela.proveedores.binance.clave-publica-webhook=" + clavePublica)
				.run(ctx -> assertThat(ctx).hasSingleBean(BinancePayAdapter.class));
	}

	@Test
	void habilitadoPeroSinCredenciales_elArranqueFallaRapido() {
		// sin api-key/secreto/clave el contexto no debe levantar: mejor caer
		// al arrancar que operar contra Binance a medio configurar
		contexto.withPropertyValues("pasarela.proveedores.binance.habilitado=true")
				.run(ctx -> assertThat(ctx).hasFailed());
	}

	@Configuration
	@Import(BinancePayAdapter.class)
	static class ConfiguracionDePrueba {

		@Bean
		Clock reloj() {
			return Clock.systemUTC();
		}

	}

}
