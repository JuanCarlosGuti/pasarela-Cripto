package com.pasarela.comercios.infraestructura.entrada.rest;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/comercios/{id}/verificacion. El motivo es opcional a
 * nivel HTTP; el dominio lo exige para RECHAZAR y SUSPENDER.
 */
public record DecisionVerificacionRequest(
		@NotBlank(message = "La decisión es obligatoria")
		String decision,
		String motivo) {
}