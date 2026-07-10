package com.pasarela.liquidaciones.dominio.excepcion;

/** La liquidación no admite conciliarse: ya fue conciliada o está en discrepancia. */
public class ConciliacionInvalidaException extends RuntimeException {

	public ConciliacionInvalidaException(String mensaje) {
		super(mensaje);
	}

}
