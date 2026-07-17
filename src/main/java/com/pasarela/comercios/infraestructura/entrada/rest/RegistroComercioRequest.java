package com.pasarela.comercios.infraestructura.entrada.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de POST /api/comercios. La validación fina (algoritmo DIAN del NIT,
 * tipos de cuenta) la hace el dominio; aquí solo se exige presencia.
 */
public record RegistroComercioRequest(
		@NotBlank(message = "La razón social es obligatoria")
		String razonSocial,
		@NotBlank(message = "El NIT es obligatorio")
		String nit,
		@NotNull(message = "La cuenta de liquidación es obligatoria")
		@Valid
		CuentaLiquidacionRequest cuentaLiquidacion,
		@NotNull(message = "Las credenciales de acceso son obligatorias")
		@Valid
		CredencialesRequest credenciales) {

	public record CredencialesRequest(
			@NotBlank(message = "El email de acceso es obligatorio")
			String email,
			@NotBlank(message = "La contraseña es obligatoria")
			String contrasena) {
	}

	public record CuentaLiquidacionRequest(
			@NotBlank(message = "El banco o billetera es obligatorio")
			String banco,
			@NotBlank(message = "El tipo de cuenta es obligatorio")
			String tipo,
			@NotBlank(message = "El número de cuenta es obligatorio")
			String numero,
			@NotBlank(message = "El titular de la cuenta es obligatorio")
			String titular) {
	}

}