package com.pasarela.compartido.infraestructura.seguridad;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Limitador de tasa de ventana deslizante (HU-022): protege los endpoints
 * públicos de martilleo. El reloj se inyecta como parámetro — mismo
 * principio de testeabilidad del dominio.
 */
class LimitadorDeTasaTest {

	private static final Instant T0 = Instant.parse("2026-07-14T10:00:00Z");

	@Test
	void permiteHastaElMaximoDentroDeLaVentana_yRechazaElSiguiente() {
		LimitadorDeTasa limitador = new LimitadorDeTasa(3, Duration.ofMinutes(1));

		assertThat(limitador.permitir("ip-1", T0)).isTrue();
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(10))).isTrue();
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(20))).isTrue();
		// la cuarta dentro del mismo minuto: rechazada
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(30))).isFalse();
	}

	@Test
	void laVentanaDesliza_alCaducarLasSolicitudesViejasVuelveAPermitir() {
		LimitadorDeTasa limitador = new LimitadorDeTasa(2, Duration.ofMinutes(1));

		assertThat(limitador.permitir("ip-1", T0)).isTrue();
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(30))).isTrue();
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(59))).isFalse();
		// a los 61s la primera solicitud salió de la ventana: hay cupo de nuevo
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(61))).isTrue();
		// pero la de los 30s sigue dentro: el cupo está lleno otra vez
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(62))).isFalse();
	}

	@Test
	void elBordeExacto_unaSolicitudDeHaceExactamenteLaVentanaYaNoCuenta() {
		LimitadorDeTasa limitador = new LimitadorDeTasa(1, Duration.ofMinutes(1));

		assertThat(limitador.permitir("ip-1", T0)).isTrue();
		// exactamente 60s después: la vieja caduca (ventana semiabierta)
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(60))).isTrue();
	}

	@Test
	void cadaClaveTieneSuPropioCupo() {
		LimitadorDeTasa limitador = new LimitadorDeTasa(1, Duration.ofMinutes(1));

		assertThat(limitador.permitir("ip-1", T0)).isTrue();
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(1))).isFalse();
		// otra IP no se ve afectada por el consumo de la primera
		assertThat(limitador.permitir("ip-2", T0.plusSeconds(1))).isTrue();
	}

	@Test
	void lasSolicitudesRechazadas_noConsumenCupo() {
		LimitadorDeTasa limitador = new LimitadorDeTasa(1, Duration.ofMinutes(1));

		assertThat(limitador.permitir("ip-1", T0)).isTrue();
		// martillar mientras está bloqueada no extiende el bloqueo:
		limitador.permitir("ip-1", T0.plusSeconds(10));
		limitador.permitir("ip-1", T0.plusSeconds(20));
		// cuando la original caduca, entra de nuevo aunque haya martillado
		assertThat(limitador.permitir("ip-1", T0.plusSeconds(61))).isTrue();
	}

	@Test
	void lasClavesInactivas_seLimpianYNoCrecenSinLimite() {
		LimitadorDeTasa limitador = new LimitadorDeTasa(1, Duration.ofMinutes(1));
		for (int i = 0; i < 5000; i++) {
			limitador.permitir("ip-" + i, T0);
		}

		// mucho después, todas caducaron: una pasada nueva las purga
		limitador.permitir("ip-nueva", T0.plus(Duration.ofMinutes(10)));

		assertThat(limitador.clavesRastreadas()).isLessThanOrEqualTo(1);
	}

}
