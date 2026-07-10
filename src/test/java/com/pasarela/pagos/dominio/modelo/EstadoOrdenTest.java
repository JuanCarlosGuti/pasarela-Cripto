package com.pasarela.pagos.dominio.modelo;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.pasarela.pagos.dominio.modelo.EstadoOrden.CONVERTIDA;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.CREADA;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.EN_REVISION;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.EXPIRADA;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.FALLIDA;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.LIQUIDADA;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.PAGO_DETECTADO;
import static com.pasarela.pagos.dominio.modelo.EstadoOrden.PENDIENTE_PAGO;
import static org.assertj.core.api.Assertions.assertThat;

class EstadoOrdenTest {

	@Test
	void desdeCreada_soloPuedeIrAPendientePago() {
		assertThat(sucesoresDe(CREADA)).containsExactly(PENDIENTE_PAGO);
	}

	@Test
	void desdePendientePago_puedeIrAPagoDetectadoExpiradaOFallida() {
		// FALLIDA desde pendiente: pago inválido, p. ej. monto errado (HU-012)
		assertThat(sucesoresDe(PENDIENTE_PAGO))
				.containsExactlyInAnyOrder(PAGO_DETECTADO, EXPIRADA, FALLIDA);
	}

	@Test
	void desdePagoDetectado_soloPuedeIrAConvertidaOFallida() {
		assertThat(sucesoresDe(PAGO_DETECTADO))
				.containsExactlyInAnyOrder(CONVERTIDA, FALLIDA);
	}

	@Test
	void desdeConvertida_soloPuedeIrALiquidada() {
		assertThat(sucesoresDe(CONVERTIDA)).containsExactly(LIQUIDADA);
	}

	@Test
	void desdeFallida_soloPuedeIrAEnRevision() {
		assertThat(sucesoresDe(FALLIDA)).containsExactly(EN_REVISION);
	}

	@Test
	void losEstadosTerminales_noTienenSucesores() {
		assertThat(sucesoresDe(LIQUIDADA)).isEmpty();
		assertThat(sucesoresDe(EXPIRADA)).isEmpty();
		assertThat(sucesoresDe(EN_REVISION)).isEmpty();
	}

	@Test
	void soloLiquidadaExpiradaYEnRevision_sonTerminales() {
		assertThat(Arrays.stream(EstadoOrden.values()).filter(EstadoOrden::esTerminal))
				.containsExactlyInAnyOrder(LIQUIDADA, EXPIRADA, EN_REVISION);
	}

	@Test
	void ningunEstado_puedeTransicionarASiMismo() {
		for (EstadoOrden estado : EstadoOrden.values()) {
			assertThat(estado.puedeTransicionarA(estado))
					.as("el estado %s no debe transicionar a sí mismo", estado)
					.isFalse();
		}
	}

	private List<EstadoOrden> sucesoresDe(EstadoOrden origen) {
		return Arrays.stream(EstadoOrden.values())
				.filter(origen::puedeTransicionarA)
				.toList();
	}

}
