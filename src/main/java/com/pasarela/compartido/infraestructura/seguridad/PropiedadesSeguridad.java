package com.pasarela.compartido.infraestructura.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de seguridad de la plataforma. El secreto JWT y las
 * credenciales del admin llegan SIEMPRE por variables de entorno en
 * producción; los defaults inocuos viven solo en los perfiles local y test.
 */
@ConfigurationProperties(prefix = "pasarela.seguridad")
public record PropiedadesSeguridad(Jwt jwt, Admin admin) {

	public record Jwt(String secreto, long minutosExpiracion) {
	}

	public record Admin(String email, String contrasena) {
	}

}
