package com.pasarela.comercios.dominio.excepcion;

/**
 * El cobro viola un tope de operación del comercio (HU-007, control de
 * cumplimiento del MVP). El intento queda en la bitácora de operaciones
 * inusuales.
 */
public class LimiteExcedidoException extends RuntimeException {

	public LimiteExcedidoException(String mensaje) {
		super(mensaje);
	}

}
