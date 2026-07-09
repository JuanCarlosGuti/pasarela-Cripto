package com.pasarela.pagos.dominio.excepcion;

/**
 * El proveedor de pagos no pudo registrar el cobro (timeout, error, caída).
 * El comercio recibe un error accionable y NO queda orden fantasma.
 */
public class ProveedorDePagoNoDisponibleException extends RuntimeException {

	public ProveedorDePagoNoDisponibleException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

	public ProveedorDePagoNoDisponibleException(String mensaje) {
		super(mensaje);
	}

}
