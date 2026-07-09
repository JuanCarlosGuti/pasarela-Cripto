package com.pasarela.compartido.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj único de la aplicación en UTC. Las reglas de dominio reciben el
 * instante como parámetro; los servicios lo toman de este bean — así los
 * tests fijan el tiempo con Clock.fixed sin tocar ninguna regla.
 */
@Configuration
public class ConfiguracionDeReloj {

	@Bean
	public Clock reloj() {
		return Clock.systemUTC();
	}

}