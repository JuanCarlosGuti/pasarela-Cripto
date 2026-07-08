package com.pasarela.compartido.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.MonedasDistintasException;
import com.pasarela.compartido.dominio.excepcion.MontoInvalidoException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {

	@Nested
	class Creacion {

		@Test
		void crear_conMontoNegativo_lanzaExcepcion() {
			assertThatThrownBy(() -> Dinero.de(new BigDecimal("-1"), Moneda.COP))
					.isInstanceOf(MontoInvalidoException.class)
					.hasMessageContaining("negativo");
		}

		@Test
		void crear_conMontoNulo_lanzaExcepcion() {
			assertThatThrownBy(() -> Dinero.de(null, Moneda.COP))
					.isInstanceOf(MontoInvalidoException.class);
		}

		@Test
		void crear_conMonedaNula_lanzaExcepcion() {
			assertThatThrownBy(() -> Dinero.de(BigDecimal.TEN, null))
					.isInstanceOf(MontoInvalidoException.class);
		}

		@Test
		void crear_conMontoCero_esValido() {
			assertThat(Dinero.cop(0).esCero()).isTrue();
		}

		@Test
		void crear_normalizaALosDecimalesDeLaMoneda() {
			// COP no tiene decimales: 1000.4 se normaliza a 1000 (redondeo bancario)
			Dinero dinero = Dinero.de(new BigDecimal("1000.4"), Moneda.COP);

			assertThat(dinero.monto()).isEqualByComparingTo("1000");
		}

		@Test
		void dosDineros_conMismoValorYMoneda_sonIguales() {
			// 1000 y 1000.00 deben ser el mismo Dinero (la escala no es identidad)
			Dinero uno = Dinero.de(new BigDecimal("1000"), Moneda.COP);
			Dinero otro = Dinero.de(new BigDecimal("1000.00"), Moneda.COP);

			assertThat(uno).isEqualTo(otro);
		}
	}

	@Nested
	class Suma {

		@Test
		void sumar_conLaMismaMoneda_devuelveLaSuma() {
			Dinero resultado = Dinero.cop(40000).sumar(Dinero.cop(2500));

			assertThat(resultado).isEqualTo(Dinero.cop(42500));
		}

		@Test
		void sumar_conMonedasDistintas_lanzaExcepcion() {
			Dinero cop = Dinero.cop(40000);
			Dinero usdt = Dinero.de(BigDecimal.TEN, Moneda.USDT);

			assertThatThrownBy(() -> cop.sumar(usdt))
					.isInstanceOf(MonedasDistintasException.class)
					.hasMessageContaining("COP")
					.hasMessageContaining("USDT");
		}
	}

	@Nested
	class Resta {

		@Test
		void restar_conLaMismaMoneda_devuelveLaDiferencia() {
			Dinero resultado = Dinero.cop(40000).restar(Dinero.cop(1000));

			assertThat(resultado).isEqualTo(Dinero.cop(39000));
		}

		@Test
		void restar_conMonedasDistintas_lanzaExcepcion() {
			Dinero cop = Dinero.cop(40000);
			Dinero usdt = Dinero.de(BigDecimal.ONE, Moneda.USDT);

			assertThatThrownBy(() -> cop.restar(usdt))
					.isInstanceOf(MonedasDistintasException.class);
		}

		@Test
		void restar_conResultadoNegativo_lanzaExcepcion() {
			// El dinero del dominio nunca es negativo (invariante del VO)
			assertThatThrownBy(() -> Dinero.cop(1000).restar(Dinero.cop(1001)))
					.isInstanceOf(MontoInvalidoException.class);
		}
	}

	@Nested
	class CalculoDePorcentaje {

		@Test
		void porcentaje_del2y5SobreCuarentaMilCop_devuelveMilCop() {
			// Criterio de aceptación literal de HU-001
			Dinero comision = Dinero.cop(40000).porcentaje(Porcentaje.de("2.5"));

			assertThat(comision).isEqualTo(Dinero.cop(1000));
		}

		@Test
		void porcentaje_conMitadExacta_redondeaAlParMasCercano() {
			// Redondeo bancario (HALF_EVEN), documentado en el VO:
			// 2.5% de 100 COP = 2.5 → 2 (el par más cercano)
			// 3.5% de 100 COP = 3.5 → 4 (el par más cercano)
			assertThat(Dinero.cop(100).porcentaje(Porcentaje.de("2.5"))).isEqualTo(Dinero.cop(2));
			assertThat(Dinero.cop(100).porcentaje(Porcentaje.de("3.5"))).isEqualTo(Dinero.cop(4));
		}

		@Test
		void porcentaje_deCero_devuelveCero() {
			assertThat(Dinero.cop(40000).porcentaje(Porcentaje.de("0"))).isEqualTo(Dinero.cop(0));
		}

		@Test
		void porcentaje_deCien_devuelveElMismoMonto() {
			assertThat(Dinero.cop(40000).porcentaje(Porcentaje.de("100"))).isEqualTo(Dinero.cop(40000));
		}
	}

}
