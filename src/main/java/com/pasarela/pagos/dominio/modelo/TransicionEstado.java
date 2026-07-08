package com.pasarela.pagos.dominio.modelo;

import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;

import java.time.Instant;

/**
 * Registro de auditoría de un cambio de estado de la orden: de dónde a dónde,
 * cuándo y (si aplica) por qué. El motivo es opcional: puede ser {@code null}
 * en transiciones que no lo requieren.
 */
public record TransicionEstado(EstadoOrden desde, EstadoOrden hacia, Instant momento, String motivo) {

	public TransicionEstado {
		if (desde == null || hacia == null || momento == null) {
			throw new OrdenInvalidaException(
					"Una transición requiere estado origen, estado destino y momento");
		}
	}

	public static TransicionEstado de(EstadoOrden desde, EstadoOrden hacia, Instant momento) {
		return new TransicionEstado(desde, hacia, momento, null);
	}

	public static TransicionEstado conMotivo(
			EstadoOrden desde, EstadoOrden hacia, Instant momento, String motivo) {
		return new TransicionEstado(desde, hacia, momento, motivo);
	}

}