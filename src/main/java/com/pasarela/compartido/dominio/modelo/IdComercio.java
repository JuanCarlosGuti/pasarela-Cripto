package com.pasarela.compartido.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.IdentificadorInvalidoException;

import java.util.UUID;

/**
 * Identificador de un comercio. Vive en el kernel compartido porque varios
 * contextos (pagos, comercios, liquidaciones) referencian al comercio por id
 * y ArchUnit prohíbe que los contextos se importen entre sí.
 */
public record IdComercio(UUID valor) {

	public IdComercio {
		if (valor == null) {
			throw new IdentificadorInvalidoException("El id del comercio no puede ser nulo");
		}
	}

	public static IdComercio generar() {
		return new IdComercio(UUID.randomUUID());
	}

	public static IdComercio de(UUID valor) {
		return new IdComercio(valor);
	}

}