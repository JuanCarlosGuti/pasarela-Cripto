package com.pasarela.pagos.dominio.excepcion;

/**
 * La orden no admite confirmar el pago: no está pendiente de pago o ya expiró.
 */
public class OrdenNoPuedeConfirmarseException extends TransicionDeEstadoInvalidaException {

	public OrdenNoPuedeConfirmarseException(String mensaje) {
		super(mensaje);
	}

}