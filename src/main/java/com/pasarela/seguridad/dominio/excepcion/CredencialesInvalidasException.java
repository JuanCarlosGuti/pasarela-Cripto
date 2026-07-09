package com.pasarela.seguridad.dominio.excepcion;

/**
 * Autenticación fallida. El mensaje es deliberadamente genérico: no revela
 * si el usuario existe o si la contraseña es incorrecta (HU-006).
 */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Credenciales inválidas");
	}

}
