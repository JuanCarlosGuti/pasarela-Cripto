package com.pasarela.liquidaciones.dominio.excepcion;

/**
 * La orden ya pertenece a otra liquidación (HU-016): una orden jamás se
 * liquida dos veces. La constraint única en BD es la última defensa.
 */
public class OrdenYaLiquidadaException extends RuntimeException {

	public OrdenYaLiquidadaException(String mensaje) {
		super(mensaje);
	}

}
