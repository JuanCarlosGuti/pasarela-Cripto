package com.pasarela.pagos.dominio.excepcion;

/**
 * Datos inválidos al crear una orden de pago o uno de sus objetos de valor.
 */
public class OrdenInvalidaException extends RuntimeException {

	public OrdenInvalidaException(String mensaje) {
		super(mensaje);
	}

}