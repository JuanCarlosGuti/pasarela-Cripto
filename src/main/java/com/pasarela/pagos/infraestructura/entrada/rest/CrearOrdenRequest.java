package com.pasarela.pagos.infraestructura.entrada.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo de POST /api/ordenes. Monto en pesos (COP). */
public record CrearOrdenRequest(
		@NotNull(message = "El monto es obligatorio")
		@Positive(message = "El monto debe ser positivo")
		Long monto) {
}
