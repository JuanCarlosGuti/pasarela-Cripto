package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdLiquidacionTest {

	@Test
	void de_envuelveElUuidRecibido() {
		UUID uuid = UUID.randomUUID();

		assertThat(IdLiquidacion.de(uuid).valor()).isEqualTo(uuid);
	}

	@Test
	void crear_conUuidNulo_lanzaExcepcion() {
		assertThatThrownBy(() -> IdLiquidacion.de(null))
				.isInstanceOf(IdentificadorInvalidoException.class);
	}

	@Test
	void generar_produceIdsDistintos() {
		assertThat(IdLiquidacion.generar()).isNotEqualTo(IdLiquidacion.generar());
	}

}
