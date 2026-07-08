package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdOrdenTest {

	@Test
	void de_envuelveElUuidRecibido() {
		UUID uuid = UUID.randomUUID();

		assertThat(IdOrden.de(uuid).valor()).isEqualTo(uuid);
	}

	@Test
	void crear_conUuidNulo_lanzaExcepcion() {
		assertThatThrownBy(() -> IdOrden.de(null))
				.isInstanceOf(IdentificadorInvalidoException.class);
	}

	@Test
	void generar_produceIdsDistintos() {
		assertThat(IdOrden.generar()).isNotEqualTo(IdOrden.generar());
	}

}