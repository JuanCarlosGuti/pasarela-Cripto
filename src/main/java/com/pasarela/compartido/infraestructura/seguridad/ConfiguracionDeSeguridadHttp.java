package com.pasarela.compartido.infraestructura.seguridad;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cadena de seguridad HTTP del monolito (HU-006): API stateless con JWT
 * firmado con HMAC-SHA256. Este paquete solo conoce RUTAS (strings), nunca
 * clases de otros contextos — la frontera de ArchUnit se respeta.
 *
 * <p>Reglas: login, registro de comercio y health son públicos; la
 * verificación de comercios exige rol ADMIN; todo lo demás, autenticado.</p>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class ConfiguracionDeSeguridadHttp {

	private static final int MINIMO_BYTES_SECRETO = 32;

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion ->
						sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(autorizacion -> autorizacion
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/comercios").permitAll()
						.requestMatchers("/api/comercios/*/verificacion").hasRole("ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
						jwt.jwtAuthenticationConverter(convertidorDeRoles())));
		return http.build();
	}

	/** El claim "rol" del token se convierte en la autoridad ROLE_<rol>. */
	private JwtAuthenticationConverter convertidorDeRoles() {
		JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
		convertidor.setJwtGrantedAuthoritiesConverter(jwt -> {
			String rol = jwt.getClaimAsString("rol");
			return rol == null
					? List.of()
					: List.of(new SimpleGrantedAuthority("ROLE_" + rol));
		});
		return convertidor;
	}

	@Bean
	JwtEncoder emisorDeJwt(PropiedadesSeguridad propiedades) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(claveHmac(propiedades)));
	}

	@Bean
	JwtDecoder decodificadorDeJwt(PropiedadesSeguridad propiedades) {
		return NimbusJwtDecoder.withSecretKey(claveHmac(propiedades))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	private SecretKey claveHmac(PropiedadesSeguridad propiedades) {
		String secreto = propiedades.jwt() == null ? null : propiedades.jwt().secreto();
		if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < MINIMO_BYTES_SECRETO) {
			throw new IllegalStateException(
					"El secreto JWT (pasarela.seguridad.jwt.secreto) debe existir y tener al menos "
							+ MINIMO_BYTES_SECRETO + " bytes; en producción llega por variable de entorno");
		}
		return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
