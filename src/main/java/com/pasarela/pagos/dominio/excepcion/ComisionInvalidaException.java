package com.pasarela.pagos.dominio.excepcion;

public class ComisionInvalidaException extends RuntimeException {

	public ComisionInvalidaException(String mensaje) {
		super(mensaje);
	}

}
