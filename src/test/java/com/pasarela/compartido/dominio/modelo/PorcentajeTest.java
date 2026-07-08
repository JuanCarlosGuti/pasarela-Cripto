package com.pasarela.compartido.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.PorcentajeInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PorcentajeTest {

	@Test
	void crear_negativo_lanzaExcepcion() {
		assertThatThrownBy(() -> Porcentaje.de("-0.1"))
				.isInstanceOf(PorcentajeInvalidoException.class);
	}

	@Test
	void crear_mayorACien_lanzaExcepcion() {
		assertThatThrownBy(() -> Porcentaje.de("100.1"))
				.isInstanceOf(PorcentajeInvalidoException.class);
	}

	@Test
	void crear_nulo_lanzaExcepcion() {
		assertThatThrownBy(() -> Porcentaje.de(null))
				.isInstanceOf(PorcentajeInvalidoException.class);
	}

	@Test
	void crear_conLosBordesCeroYCien_esValido() {
		assertThat(Porcentaje.de("0").valor()).isEqualByComparingTo("0");
		assertThat(Porcentaje.de("100").valor()).isEqualByComparingTo("100");
	}

	@Test
	void comoFraccion_de2y5_devuelve0punto025() {
		assertThat(Porcentaje.de("2.5").comoFraccion()).isEqualByComparingTo("0.025");
	}

	@Test
	void dosPorcentajes_conMismoValor_sonIguales() {
		assertThat(Porcentaje.de("2.5")).isEqualTo(Porcentaje.de("2.50"));
	}

}
