package com.pasarela.compartido.dominio.excepcion;

public class MontoInvalidoException extends RuntimeException {

	public MontoInvalidoException(String mensaje) {
		super(mensaje);
	}

}