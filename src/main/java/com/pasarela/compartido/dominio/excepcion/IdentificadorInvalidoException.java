package com.pasarela.compartido.dominio.excepcion;

public class IdentificadorInvalidoException extends RuntimeException {

	public IdentificadorInvalidoException(String mensaje) {
		super(mensaje);
	}

}