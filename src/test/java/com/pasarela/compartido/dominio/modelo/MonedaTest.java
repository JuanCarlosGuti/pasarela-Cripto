package com.pasarela.compartido.dominio.modelo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonedaTest {

	@Test
	void cop_noTieneDecimales() {
		assertThat(Moneda.COP.decimales()).isZero();
	}

	@Test
	void usdt_tieneSeisDecimales() {
		assertThat(Moneda.USDT.decimales()).isEqualTo(6);
	}

}
