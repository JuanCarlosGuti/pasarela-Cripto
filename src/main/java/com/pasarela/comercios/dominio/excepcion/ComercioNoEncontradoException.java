package com.pasarela.comercios.dominio.excepcion;

public class ComercioNoEncontradoException extends RuntimeException {

	public ComercioNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}