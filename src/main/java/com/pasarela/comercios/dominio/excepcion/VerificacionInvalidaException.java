package com.pasarela.comercios.dominio.excepcion;

/**
 * La decisión de verificación no es válida en el estado actual del comercio
 * (p. ej. verificar un SUSPENDIDO sin reactivación explícita, HU-005).
 */
public class VerificacionInvalidaException extends RuntimeException {

	public VerificacionInvalidaException(String mensaje) {
		super(mensaje);
	}

}