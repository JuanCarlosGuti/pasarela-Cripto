package com.pasarela.seguridad.dominio.excepcion;

/** Datos inválidos al crear una cuenta de usuario. */
public class UsuarioInvalidoException extends RuntimeException {

	public UsuarioInvalidoException(String mensaje) {
		super(mensaje);
	}

}
