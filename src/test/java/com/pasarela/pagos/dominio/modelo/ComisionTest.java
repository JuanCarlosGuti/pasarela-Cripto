package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.Moneda;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.pagos.dominio.excepcion.ComisionInvalidaException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComisionTest {

	private static final Comision DOS_Y_MEDIO_PORCIENTO = new Comision(Porcentaje.de("2.5"));

	@Nested
	class Creacion {

		@Test
		void crear_conTasaNula_lanzaExcepcion() {
			assertThatThrownBy(() -> new Comision(null))
					.isInstanceOf(ComisionInvalidaException.class);
		}
	}

	@Nested
	class Calculo {

		@Test
		void calcular_2y5PorcientoSobre40000Cop_devuelve1000Cop() {
			// Criterio de aceptación literal de HU-003
			assertThat(DOS_Y_MEDIO_PORCIENTO.calcular(Dinero.cop(40000)))
					.isEqualTo(Dinero.cop(1000));
		}

		@Test
		void netoParaElComercio_sobre40000Cop_devuelve39000Cop() {
			assertThat(DOS_Y_MEDIO_PORCIENTO.netoParaElComercio(Dinero.cop(40000)))
					.isEqualTo(Dinero.cop(39000));
		}

		@Test
		void comisionMasNeto_siempreSumanElMontoOriginal() {
			// La plataforma jamás "pierde" ni "crea" un peso al partir el monto
			Dinero monto = Dinero.cop(33333);

			Dinero comision = DOS_Y_MEDIO_PORCIENTO.calcular(monto);
			Dinero neto = DOS_Y_MEDIO_PORCIENTO.netoParaElComercio(monto);

			assertThat(comision.sumar(neto)).isEqualTo(monto);
		}

		@Test
		void calcular_conMitadExacta_usaRedondeoBancario() {
			// Documentado en Dinero: HALF_EVEN. 2.5% de 100 = 2.5 → 2 (par más cercano)
			assertThat(DOS_Y_MEDIO_PORCIENTO.calcular(Dinero.cop(100)))
					.isEqualTo(Dinero.cop(2));
		}

		@Test
		void calcular_conservaLaMonedaDelMonto() {
			Dinero enUsdt = Dinero.de(new BigDecimal("100"), Moneda.USDT);

			assertThat(DOS_Y_MEDIO_PORCIENTO.calcular(enUsdt).moneda()).isEqualTo(Moneda.USDT);
		}

		@Test
		void calcular_conTasaCero_devuelveCeroYNetoCompleto() {
			Comision sinComision = new Comision(Porcentaje.de("0"));

			assertThat(sinComision.calcular(Dinero.cop(40000)).esCero()).isTrue();
			assertThat(sinComision.netoParaElComercio(Dinero.cop(40000))).isEqualTo(Dinero.cop(40000));
		}
	}

}