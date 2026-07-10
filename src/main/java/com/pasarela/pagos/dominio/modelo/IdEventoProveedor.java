package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;

import java.util.UUID;

/** Identificador interno del evento crudo del proveedor. */
public record IdEventoProveedor(UUID valor) {

	public IdEventoProveedor {
		if (valor == null) {
			throw new IdentificadorInvalidoException("El id del evento no puede ser nulo");
		}
	}

	public static IdEventoProveedor generar() {
		return new IdEventoProveedor(UUID.randomUUID());
	}

	public static IdEventoProveedor de(UUID valor) {
		return new IdEventoProveedor(valor);
	}

}
