package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.puerto.entrada.GenerarComprobanteUseCase.Comprobante;

import java.time.Instant;
import java.util.UUID;

/**
 * Comprobante de venta para el comercio (HU-020): el soporte que muestra al
 * cliente y entrega al contador. {@code liquidadaEn} es null mientras el
 * proveedor no haya liquidado los COP.
 */
public record ComprobanteResponse(
		UUID numeroComprobante,
		String referencia,
		long monto,
		String moneda,
		String estado,
		Instant creadaEn,
		Instant pagoDetectadoEn,
		Instant liquidadaEn,
		Instant emitidoEn) {

	static ComprobanteResponse de(Comprobante comprobante) {
		return new ComprobanteResponse(
				comprobante.numero(),
				comprobante.referencia(),
				comprobante.monto().monto().longValue(),
				comprobante.monto().moneda().name(),
				comprobante.estado().name(),
				comprobante.creadaEn(),
				comprobante.pagoDetectadoEn(),
				comprobante.liquidadaEn(),
				comprobante.emitidoEn());
	}

}
