package com.pasarela.liquidaciones.dominio.excepcion;

public class LiquidacionNoEncontradaException extends RuntimeException {

	public LiquidacionNoEncontradaException(String mensaje) {
		super(mensaje);
	}

}
