package com.pasarela.comercios.dominio.excepcion;

public class NitInvalidoException extends RuntimeException {

	public NitInvalidoException(String mensaje) {
		super(mensaje);
	}

}