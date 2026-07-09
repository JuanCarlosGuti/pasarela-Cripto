package com.pasarela.comercios.dominio.excepcion;

/** Ya existe un comercio con ese NIT: el registro se rechaza (HU-004). */
public class ComercioYaRegistradoException extends RuntimeException {

	public ComercioYaRegistradoException(String mensaje) {
		super(mensaje);
	}

}