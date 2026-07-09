package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.time.Instant;
import java.util.UUID;

/**
 * Representación pública del comercio. No expone la cuenta de liquidación
 * (dato sensible) ni el motivo de decisiones (se consulta por canales de
 * administración cuando exista el rol, HU-006).
 */
public record ComercioResponse(
		UUID id,
		String razonSocial,
		String nit,
		String estadoVerificacion,
		Instant registradoEn,
		LimitesResponse limites) {

	/** Topes de operación en pesos (COP). */
	public record LimitesResponse(long topePorTransaccion, long topeMensual) {
	}

	static ComercioResponse de(Comercio comercio) {
		return new ComercioResponse(
				comercio.id().valor(),
				comercio.razonSocial(),
				comercio.nit().completo(),
				comercio.estadoVerificacion().name(),
				comercio.registradoEn(),
				new LimitesResponse(
						comercio.limites().topePorTransaccion().monto().longValue(),
						comercio.limites().topeMensual().monto().longValue()));
	}

}