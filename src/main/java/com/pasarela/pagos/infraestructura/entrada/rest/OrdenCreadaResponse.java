package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.OrdenCreada;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de la creación del cobro: lo que la caja del comercio necesita
 * para mostrar el QR. No expone datos internos del proveedor.
 */
public record OrdenCreadaResponse(
		UUID id,
		String referencia,
		String estado,
		long monto,
		Instant expiraEn,
		QrResponse qr) {

	public record QrResponse(String contenido, String deeplink) {
	}

	static OrdenCreadaResponse de(OrdenCreada creada) {
		return new OrdenCreadaResponse(
				creada.orden().id().valor(),
				creada.orden().referencia().valor(),
				creada.orden().estado().name(),
				creada.orden().monto().monto().longValue(),
				creada.orden().expiraEn(),
				new QrResponse(creada.cobro().contenidoQr(), creada.cobro().deeplink()));
	}

}
