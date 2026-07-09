package com.pasarela.comercios.dominio.excepcion;

/** El comercio no puede recibir cobros: no está verificado o está suspendido. */
public class ComercioNoAutorizadoException extends RuntimeException {

	public ComercioNoAutorizadoException(String mensaje) {
		super(mensaje);
	}

}
