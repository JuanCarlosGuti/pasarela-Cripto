package com.pasarela.pagos.dominio.excepcion;

/**
 * Se intentó una transición que la máquina de estados de la orden no permite
 * (ver la matriz de docs/04-modelo-de-dominio.md).
 */
public class TransicionDeEstadoInvalidaException extends RuntimeException {

	public TransicionDeEstadoInvalidaException(String mensaje) {
		super(mensaje);
	}

}