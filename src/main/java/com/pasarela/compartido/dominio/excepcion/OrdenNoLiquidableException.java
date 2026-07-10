package com.pasarela.compartido.dominio.excepcion;

/**
 * La orden no puede incluirse en una liquidación: no existe, no es del
 * comercio indicado o no está CONVERTIDA (HU-016).
 */
public class OrdenNoLiquidableException extends RuntimeException {

	public OrdenNoLiquidableException(String mensaje) {
		super(mensaje);
	}

}
