package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.TransicionEstado;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Detalle de la orden para su dueño: estado, monto y auditoría de transiciones. */
public record OrdenDetalleResponse(
		UUID id,
		String referencia,
		String estado,
		long monto,
		Instant creadaEn,
		Instant expiraEn,
		List<TransicionResponse> transiciones) {

	public record TransicionResponse(String desde, String hacia, Instant momento) {
	}

	static OrdenDetalleResponse de(OrdenDePago orden) {
		return new OrdenDetalleResponse(
				orden.id().valor(),
				orden.referencia().valor(),
				orden.estado().name(),
				orden.monto().monto().longValue(),
				orden.creadaEn(),
				orden.expiraEn(),
				orden.historial().stream().map(OrdenDetalleResponse::transicion).toList());
	}

	private static TransicionResponse transicion(TransicionEstado transicion) {
		return new TransicionResponse(
				transicion.desde().name(), transicion.hacia().name(), transicion.momento());
	}

}
