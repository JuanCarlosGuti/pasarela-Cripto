package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta del registro. No expone la cuenta de liquidación: es dato
 * sensible y el cliente ya la conoce.
 */
public record ComercioRegistradoResponse(
		UUID id,
		String razonSocial,
		String nit,
		String estadoVerificacion,
		Instant registradoEn) {

	static ComercioRegistradoResponse de(Comercio comercio) {
		return new ComercioRegistradoResponse(
				comercio.id().valor(),
				comercio.razonSocial(),
				comercio.nit().completo(),
				comercio.estadoVerificacion().name(),
				comercio.registradoEn());
	}

}