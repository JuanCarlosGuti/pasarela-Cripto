package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdEventoProveedorTest {

	@Test
	void de_envuelveElUuidRecibido() {
		UUID uuid = UUID.randomUUID();

		assertThat(IdEventoProveedor.de(uuid).valor()).isEqualTo(uuid);
	}

	@Test
	void crear_conUuidNulo_lanzaExcepcion() {
		assertThatThrownBy(() -> IdEventoProveedor.de(null))
				.isInstanceOf(IdentificadorInvalidoException.class);
	}

	@Test
	void generar_produceIdsDistintos() {
		assertThat(IdEventoProveedor.generar()).isNotEqualTo(IdEventoProveedor.generar());
	}

}
