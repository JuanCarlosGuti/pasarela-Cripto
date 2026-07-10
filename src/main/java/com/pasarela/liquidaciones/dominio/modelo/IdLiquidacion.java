package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;

import java.util.UUID;

/** Identificador de una liquidación. */
public record IdLiquidacion(UUID valor) {

	public IdLiquidacion {
		if (valor == null) {
			throw new IdentificadorInvalidoException("El id de la liquidación no puede ser nulo");
		}
	}

	public static IdLiquidacion generar() {
		return new IdLiquidacion(UUID.randomUUID());
	}

	public static IdLiquidacion de(UUID valor) {
		return new IdLiquidacion(valor);
	}

}
