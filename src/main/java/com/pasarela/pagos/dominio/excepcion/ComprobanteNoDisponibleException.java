package com.pasarela.pagos.dominio.excepcion;

/**
 * Se pidió el comprobante de una orden cuyo pago aún no fue detectado
 * (HU-020): pendientes, expiradas, fallidas y en revisión no tienen soporte
 * de venta que emitir.
 */
public class ComprobanteNoDisponibleException extends RuntimeException {

	public ComprobanteNoDisponibleException(String mensaje) {
		super(mensaje);
	}

}
