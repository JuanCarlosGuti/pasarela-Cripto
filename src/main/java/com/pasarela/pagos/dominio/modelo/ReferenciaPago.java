package com.pasarela.pagos.dominio.modelo;

import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;

import java.util.UUID;

/**
 * Referencia única que viaja al proveedor y regresa en el webhook; permite
 * casar el pago con la orden. No contiene datos del comercio.
 */
public record ReferenciaPago(String valor) {

	public ReferenciaPago {
		if (valor == null || valor.isBlank()) {
			throw new OrdenInvalidaException("La referencia de pago no puede estar vacía");
		}
	}

	public static ReferenciaPago generar() {
		return new ReferenciaPago(UUID.randomUUID().toString());
	}

}