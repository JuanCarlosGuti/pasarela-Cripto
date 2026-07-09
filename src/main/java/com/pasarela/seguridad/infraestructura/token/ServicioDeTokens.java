package com.pasarela.seguridad.infraestructura.token;

import com.pasarela.compartido.infraestructura.seguridad.PropiedadesSeguridad;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite el JWT de la plataforma. Claims mínimos y nunca sensibles: email
 * (subject), rol y — solo para usuarios COMERCIO — el id de su comercio.
 */
@Component
public class ServicioDeTokens {

	private final JwtEncoder emisor;
	private final PropiedadesSeguridad propiedades;
	private final Clock reloj;

	public ServicioDeTokens(JwtEncoder emisor, PropiedadesSeguridad propiedades, Clock reloj) {
		this.emisor = emisor;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	public TokenEmitido emitir(Usuario usuario) {
		Instant ahora = reloj.instant();
		Instant expiraEn = ahora.plus(Duration.ofMinutes(propiedades.jwt().minutosExpiracion()));
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.subject(usuario.email())
				.issuedAt(ahora)
				.expiresAt(expiraEn)
				.claim("rol", usuario.rol().name());
		if (usuario.comercioId() != null) {
			claims.claim("comercioId", usuario.comercioId().valor().toString());
		}
		String token = emisor.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
		return new TokenEmitido(token, expiraEn);
	}

	public record TokenEmitido(String token, Instant expiraEn) {
	}

}
