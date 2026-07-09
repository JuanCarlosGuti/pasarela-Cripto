package com.pasarela.comercios.dominio.excepcion;

/** Datos inválidos al crear un comercio o uno de sus objetos de valor. */
public class ComercioInvalidoException extends RuntimeException {

	public ComercioInvalidoException(String mensaje) {
		super(mensaje);
	}

}