package com.pasarela.pagos.dominio.modelo;

import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransicionEstadoTest {

	private static final Instant MOMENTO = Instant.parse("2026-07-08T10:00:00Z");

	@Test
	void de_creaLaTransicionSinMotivo() {
		TransicionEstado transicion = TransicionEstado.de(
				EstadoOrden.CREADA, EstadoOrden.PENDIENTE_PAGO, MOMENTO);

		assertThat(transicion.desde()).isEqualTo(EstadoOrden.CREADA);
		assertThat(transicion.hacia()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
		assertThat(transicion.momento()).isEqualTo(MOMENTO);
		assertThat(transicion.motivo()).isNull();
	}

	@Test
	void conMotivo_creaLaTransicionConElMotivo() {
		TransicionEstado transicion = TransicionEstado.conMotivo(
				EstadoOrden.PAGO_DETECTADO, EstadoOrden.FALLIDA, MOMENTO, "pago inválido");

		assertThat(transicion.desde()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
		assertThat(transicion.hacia()).isEqualTo(EstadoOrden.FALLIDA);
		assertThat(transicion.motivo()).isEqualTo("pago inválido");
	}

	@Test
	void crear_conOrigenDestinoOMomentoNulos_lanzaExcepcion() {
		assertThatThrownBy(() -> TransicionEstado.de(null, EstadoOrden.PENDIENTE_PAGO, MOMENTO))
				.isInstanceOf(OrdenInvalidaException.class);
		assertThatThrownBy(() -> TransicionEstado.de(EstadoOrden.CREADA, null, MOMENTO))
				.isInstanceOf(OrdenInvalidaException.class);
		assertThatThrownBy(() -> TransicionEstado.de(EstadoOrden.CREADA, EstadoOrden.PENDIENTE_PAGO, null))
				.isInstanceOf(OrdenInvalidaException.class);
	}

}