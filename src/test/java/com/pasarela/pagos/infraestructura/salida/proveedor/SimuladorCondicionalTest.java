package com.pasarela.pagos.infraestructura.salida.proveedor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-006: el simulador solo existe si la propiedad lo habilita. Producción
 * jamás define la propiedad → el bean no se crea (y sin ningún adaptador
 * del puerto, el arranque falla rápido: mejor que un simulador en prod).
 */
class SimuladorCondicionalTest {

	private final ApplicationContextRunner contexto = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(ConfiguracionDePrueba.class);

	@Test
	void sinLaPropiedad_elBeanNoExiste_comoEnProduccion() {
		contexto.run(ctx -> assertThat(ctx).doesNotHaveBean(ProveedorDePagoSimulado.class));
	}

	@Test
	void conLaPropiedadEnFalse_elBeanTampocoExiste() {
		contexto.withPropertyValues("pasarela.proveedores.simulado.habilitado=false")
				.run(ctx -> assertThat(ctx).doesNotHaveBean(ProveedorDePagoSimulado.class));
	}

	@Test
	void conLaPropiedadHabilitada_elBeanExiste() {
		contexto.withPropertyValues(
						"pasarela.proveedores.simulado.habilitado=true",
						"pasarela.proveedores.simulado.secreto-webhook=secreto-de-prueba")
				.run(ctx -> assertThat(ctx).hasSingleBean(ProveedorDePagoSimulado.class));
	}

	@Configuration
	@Import(ProveedorDePagoSimulado.class)
	static class ConfiguracionDePrueba {
	}

}
