package com.pasarela.seguridad.dominio.excepcion;

/** Ya existe una cuenta con ese email. */
public class UsuarioYaExisteException extends RuntimeException {

	public UsuarioYaExisteException(String mensaje) {
		super(mensaje);
	}

}
