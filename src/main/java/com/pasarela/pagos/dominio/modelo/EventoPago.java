package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;

import java.time.Instant;

/**
 * Datos del pago confirmado por el proveedor: con qué referencia se casa,
 * cuánto se pagó (puede venir en moneda cripto) y cuándo lo reportó el
 * proveedor.
 */
public record EventoPago(ReferenciaPago referencia, Dinero monto, Instant pagadoEn) {

	public EventoPago {
		if (referencia == null || monto == null || pagadoEn == null) {
			throw new OrdenInvalidaException(
					"Un evento de pago requiere referencia, monto y fecha de pago");
		}
	}

}