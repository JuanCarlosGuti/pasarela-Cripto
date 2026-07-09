package com.pasarela.pagos.dominio.excepcion;

/**
 * La orden no existe o el solicitante no es su dueño: la respuesta es la
 * misma en ambos casos para no filtrar existencia (HU-009).
 */
public class OrdenNoEncontradaException extends RuntimeException {

	public OrdenNoEncontradaException(String mensaje) {
		super(mensaje);
	}

}
