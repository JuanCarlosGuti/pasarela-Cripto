package com.pasarela.compartido.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * Límite de tasa por IP en los endpoints PÚBLICOS (HU-022): login (fuerza
 * bruta de credenciales), webhooks (inundación de eventos falsos) y la
 * consulta pública del pagador (enumeración/scraping). Los endpoints
 * autenticados no pasan por aquí: el JWT ya les pone costo.
 *
 * <p>Corre ANTES de la cadena de seguridad ({@code HIGHEST_PRECEDENCE}):
 * rechazar barato, sin tocar crypto ni base de datos. Responde 429 con
 * {@code Retry-After}. La clave es la IP del cliente — si algún día hay un
 * proxy/balanceador delante, confiar en {@code X-Forwarded-For} SOLO si lo
 * setea nuestro proxy (hoy se toma el primer valor, documentado en
 * docs/09).</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "pasarela.seguridad.limite-tasa.habilitado", havingValue = "true")
public class FiltroDeLimiteDeTasa extends OncePerRequestFilter {

	private final LimitadorDeTasa limitadorDeLogin;
	private final LimitadorDeTasa limitadorDeWebhooks;
	private final LimitadorDeTasa limitadorDeConsultaPublica;
	private final long ventanaSegundos;
	private final Clock reloj;

	/**
	 * El reloj llega como {@link ObjectProvider} con fallback al del sistema:
	 * los slices web ({@code @WebMvcTest}) instancian los filtros pero no
	 * cargan la configuración del reloj — el filtro no debe tumbar esos
	 * contextos por una dependencia que aquí es accesoria.
	 */
	public FiltroDeLimiteDeTasa(
			@Value("${pasarela.seguridad.limite-tasa.ventana-segundos:60}") long ventanaSegundos,
			@Value("${pasarela.seguridad.limite-tasa.login.maximo:10}") int maximoLogin,
			@Value("${pasarela.seguridad.limite-tasa.webhook.maximo:120}") int maximoWebhooks,
			@Value("${pasarela.seguridad.limite-tasa.consulta-publica.maximo:60}") int maximoConsultaPublica,
			ObjectProvider<Clock> reloj) {
		Duration ventana = Duration.ofSeconds(ventanaSegundos);
		this.ventanaSegundos = ventanaSegundos;
		this.limitadorDeLogin = new LimitadorDeTasa(maximoLogin, ventana);
		this.limitadorDeWebhooks = new LimitadorDeTasa(maximoWebhooks, ventana);
		this.limitadorDeConsultaPublica = new LimitadorDeTasa(maximoConsultaPublica, ventana);
		this.reloj = reloj.getIfAvailable(Clock::systemUTC);
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest solicitud,
			@NonNull HttpServletResponse respuesta, @NonNull FilterChain cadena)
			throws ServletException, IOException {
		LimitadorDeTasa limitador = limitadorPara(solicitud);
		if (limitador == null || limitador.permitir(ipDelCliente(solicitud), reloj.instant())) {
			cadena.doFilter(solicitud, respuesta);
			return;
		}
		respuesta.setStatus(429);
		respuesta.setHeader("Retry-After", String.valueOf(ventanaSegundos));
		respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
		respuesta.getWriter().write(
				"{\"mensaje\": \"Demasiadas solicitudes; intenta de nuevo en un momento\"}");
	}

	/** Null = la ruta no está limitada y la solicitud sigue de largo. */
	private LimitadorDeTasa limitadorPara(HttpServletRequest solicitud) {
		String ruta = solicitud.getRequestURI();
		String metodo = solicitud.getMethod();
		if ("POST".equals(metodo) && "/api/auth/login".equals(ruta)) {
			return limitadorDeLogin;
		}
		if ("POST".equals(metodo) && ruta.startsWith("/api/webhooks/")) {
			return limitadorDeWebhooks;
		}
		if ("GET".equals(metodo) && ruta.startsWith("/api/pagos/")) {
			return limitadorDeConsultaPublica;
		}
		return null;
	}

	private static String ipDelCliente(HttpServletRequest solicitud) {
		String reenviadaPor = solicitud.getHeader("X-Forwarded-For");
		if (reenviadaPor == null || reenviadaPor.isBlank()) {
			return solicitud.getRemoteAddr();
		}
		return reenviadaPor.split(",")[0].trim();
	}

}
