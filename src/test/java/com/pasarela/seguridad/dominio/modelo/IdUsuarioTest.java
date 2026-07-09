package com.pasarela.seguridad.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdUsuarioTest {

	@Test
	void de_envuelveElUuidRecibido() {
		UUID uuid = UUID.randomUUID();

		assertThat(IdUsuario.de(uuid).valor()).isEqualTo(uuid);
	}

	@Test
	void crear_conUuidNulo_lanzaExcepcion() {
		assertThatThrownBy(() -> IdUsuario.de(null))
				.isInstanceOf(IdentificadorInvalidoException.class);
	}

	@Test
	void generar_produceIdsDistintos() {
		assertThat(IdUsuario.generar()).isNotEqualTo(IdUsuario.generar());
	}

}
