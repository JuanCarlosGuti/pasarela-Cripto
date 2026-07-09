package com.pasarela.comercios.infraestructura.entrada.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de PUT /api/comercios/{id}/limites. Montos en pesos (COP). */
public record ActualizacionLimitesRequest(
		@NotNull(message = "El tope por transacción es obligatorio")
		@Positive(message = "El tope por transacción debe ser positivo")
		Long topePorTransaccion,
		@NotNull(message = "El tope mensual es obligatorio")
		@Positive(message = "El tope mensual debe ser positivo")
		Long topeMensual) {
}
