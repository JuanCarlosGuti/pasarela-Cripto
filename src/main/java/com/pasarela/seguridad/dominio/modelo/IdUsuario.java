package com.pasarela.seguridad.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;

import java.util.UUID;

/** Identificador de una cuenta de usuario. */
public record IdUsuario(UUID valor) {

	public IdUsuario {
		if (valor == null) {
			throw new IdentificadorInvalidoException("El id del usuario no puede ser nulo");
		}
	}

	public static IdUsuario generar() {
		return new IdUsuario(UUID.randomUUID());
	}

	public static IdUsuario de(UUID valor) {
		return new IdUsuario(valor);
	}

}
