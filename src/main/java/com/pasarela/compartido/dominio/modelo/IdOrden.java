package com.pasarela.compartido.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;

import java.util.UUID;

/** Identificador de una orden de pago (evita mezclar ids de otras entidades). */
public record IdOrden(UUID valor) {

	public IdOrden {
		if (valor == null) {
			throw new IdentificadorInvalidoException("El id de la orden no puede ser nulo");
		}
	}

	public static IdOrden generar() {
		return new IdOrden(UUID.randomUUID());
	}

	public static IdOrden de(UUID valor) {
		return new IdOrden(valor);
	}

}