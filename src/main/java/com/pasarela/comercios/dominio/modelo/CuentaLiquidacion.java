package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;

/**
 * Cuenta del comercio donde el proveedor de rampa liquida los COP.
 * Siempre es una cuenta DEL COMERCIO, jamás de la plataforma (REGLA DE ORO:
 * ningún dinero de terceros pasa por cuentas nuestras).
 */
public record CuentaLiquidacion(TipoCuenta tipo, String numero, String titular) {

	public CuentaLiquidacion {
		if (tipo == null) {
			throw new ComercioInvalidoException("La cuenta de liquidación requiere un tipo");
		}
		if (numero == null || numero.isBlank()) {
			throw new ComercioInvalidoException("La cuenta de liquidación requiere un número");
		}
		if (!numero.chars().allMatch(Character::isDigit)) {
			throw new ComercioInvalidoException(
					"El número de la cuenta de liquidación solo admite dígitos");
		}
		if (titular == null || titular.isBlank()) {
			throw new ComercioInvalidoException("La cuenta de liquidación requiere un titular");
		}
	}

}