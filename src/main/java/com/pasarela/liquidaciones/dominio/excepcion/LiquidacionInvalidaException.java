package com.pasarela.liquidaciones.dominio.excepcion;

/** Datos inválidos al registrar una liquidación (HU-016). */
public class LiquidacionInvalidaException extends RuntimeException {

	public LiquidacionInvalidaException(String mensaje) {
		super(mensaje);
	}

}
