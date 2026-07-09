package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.LimiteExcedidoException;
import com.pasarela.compartido.dominio.modelo.Dinero;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitesOperacionTest {

	private static final LimitesOperacion LIMITES = new LimitesOperacion(
			Dinero.cop(2_000_000), Dinero.cop(20_000_000));

	@Nested
	class Creacion {

		@Test
		void losLimitesPorDefectoDelMvp_sonDosMillonesYVeinteMillones() {
			LimitesOperacion porDefecto = LimitesOperacion.porDefecto();

			assertThat(porDefecto.topePorTransaccion()).isEqualTo(Dinero.cop(2_000_000));
			assertThat(porDefecto.topeMensual()).isEqualTo(Dinero.cop(20_000_000));
		}

		@Test
		void crear_conTopesNulos_lanzaExcepcion() {
			assertThatThrownBy(() -> new LimitesOperacion(null, Dinero.cop(1)))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> new LimitesOperacion(Dinero.cop(1), null))
					.isInstanceOf(ComercioInvalidoException.class);
		}

		@Test
		void crear_conTopesEnCero_lanzaExcepcion() {
			assertThatThrownBy(() -> new LimitesOperacion(Dinero.cop(0), Dinero.cop(100)))
					.isInstanceOf(ComercioInvalidoException.class)
					.hasMessageContaining("mayores que cero");
		}

		@Test
		void crear_conTopePorTransaccionMayorAlMensual_lanzaExcepcion() {
			assertThatThrownBy(() -> new LimitesOperacion(
					Dinero.cop(2_000_000), Dinero.cop(1_000_000)))
					.isInstanceOf(ComercioInvalidoException.class)
					.hasMessageContaining("mensual");
		}
	}

	@Nested
	class ValidacionDeCobros {

		@Test
		void unCobroEnElTopeExacto_pasa_yUnPesoMas_seRechaza() {
			// Criterio literal de HU-007: tope $2.000.000, cobro $2.000.001
			assertThatCode(() -> LIMITES.validarCobro(Dinero.cop(2_000_000), Dinero.cop(0)))
					.doesNotThrowAnyException();

			assertThatThrownBy(() -> LIMITES.validarCobro(Dinero.cop(2_000_001), Dinero.cop(0)))
					.isInstanceOf(LimiteExcedidoException.class)
					.hasMessageContaining("tope por transacción")
					.hasMessageContaining("2000000");
		}

		@Test
		void unCobroQueExcederiaElTopeMensual_seRechaza() {
			// acumulado 19.5M + cobro 600k = 20.1M > tope 20M
			assertThatThrownBy(() -> LIMITES.validarCobro(
					Dinero.cop(600_000), Dinero.cop(19_500_000)))
					.isInstanceOf(LimiteExcedidoException.class)
					.hasMessageContaining("tope mensual");
		}

		@Test
		void unCobroQueLlegaJustoAlTopeMensual_pasa() {
			assertThatCode(() -> LIMITES.validarCobro(
					Dinero.cop(500_000), Dinero.cop(19_500_000)))
					.doesNotThrowAnyException();
		}

		@Test
		void validar_conDatosNulos_lanzaExcepcion() {
			assertThatThrownBy(() -> LIMITES.validarCobro(null, Dinero.cop(0)))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> LIMITES.validarCobro(Dinero.cop(1), null))
					.isInstanceOf(ComercioInvalidoException.class);
		}
	}

}
