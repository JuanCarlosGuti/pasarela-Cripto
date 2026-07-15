package com.pasarela.compartido.infraestructura.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Limitador de tasa de ventana deslizante en memoria (HU-022).
 *
 * <p>Decisiones de diseño:</p>
 * <ul>
 *   <li><b>En memoria y sincronizado:</b> el sistema es un monolito de una
 *       sola instancia (ADR-002); un almacén distribuido (Redis) sería
 *       sobre-ingeniería hoy. Si el despliegue escala a réplicas, este es el
 *       único punto a reemplazar.</li>
 *   <li><b>Ventana semiabierta:</b> una solicitud de hace exactamente la
 *       ventana ya no cuenta.</li>
 *   <li><b>Los rechazos no consumen cupo:</b> martillar durante el bloqueo
 *       no lo extiende; el atacante no puede auto-perpetuar el 429 de una
 *       víctima con su misma IP compartida más de lo configurado.</li>
 *   <li><b>El reloj entra por parámetro</b> — testeabilidad, como en el
 *       dominio.</li>
 * </ul>
 */
public class LimitadorDeTasa {

	private final int maximoSolicitudes;
	private final Duration ventana;
	private final Map<String, Deque<Instant>> solicitudesPorClave = new HashMap<>();
	private Instant ultimaPurga = Instant.MIN;

	public LimitadorDeTasa(int maximoSolicitudes, Duration ventana) {
		if (maximoSolicitudes < 1 || ventana.isZero() || ventana.isNegative()) {
			throw new IllegalArgumentException(
					"El límite de tasa requiere un máximo >= 1 y una ventana positiva");
		}
		this.maximoSolicitudes = maximoSolicitudes;
		this.ventana = ventana;
	}

	/** ¿Esta clave (IP) todavía tiene cupo en su ventana? */
	public synchronized boolean permitir(String clave, Instant ahora) {
		purgarClavesInactivasSiToca(ahora);
		Deque<Instant> solicitudes =
				solicitudesPorClave.computeIfAbsent(clave, k -> new ArrayDeque<>());
		descartarCaducadas(solicitudes, ahora);
		if (solicitudes.size() >= maximoSolicitudes) {
			return false;
		}
		solicitudes.addLast(ahora);
		return true;
	}

	private void descartarCaducadas(Deque<Instant> solicitudes, Instant ahora) {
		Instant limite = ahora.minus(ventana);
		while (!solicitudes.isEmpty() && !solicitudes.peekFirst().isAfter(limite)) {
			solicitudes.removeFirst();
		}
	}

	/**
	 * A lo sumo una vez por ventana, elimina las claves sin solicitudes
	 * vigentes: la memoria queda acotada por el tráfico de UNA ventana, no
	 * por el histórico de IPs vistas.
	 */
	private void purgarClavesInactivasSiToca(Instant ahora) {
		if (Duration.between(ultimaPurga, ahora).compareTo(ventana) < 0) {
			return;
		}
		ultimaPurga = ahora;
		solicitudesPorClave.values().forEach(cola -> descartarCaducadas(cola, ahora));
		solicitudesPorClave.values().removeIf(Deque::isEmpty);
	}

	int clavesRastreadas() {
		return solicitudesPorClave.size();
	}

}
