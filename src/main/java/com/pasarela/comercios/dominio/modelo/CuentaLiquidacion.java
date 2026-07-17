package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;

/**
 * Cuenta del comercio donde el proveedor de rampa liquida los COP.
 * Siempre es una cuenta DEL COMERCIO, jamás de la plataforma (REGLA DE ORO:
 * ningún dinero de terceros pasa por cuentas nuestras).
 *
 * <p>HU-027: el banco/billetera (Nequi, Bancolombia...) viaja aparte del
 * tipo — el proveedor de payout lo va a exigir. Las billeteras se registran
 * como AHORROS por convención de la plataforma (decide el cliente/UX).</p>
 */
public record CuentaLiquidacion(String banco, TipoCuenta tipo, String numero, String titular) {

	public CuentaLiquidacion {
		if (banco == null || banco.isBlank()) {
			throw new ComercioInvalidoException(
					"La cuenta de liquidación requiere el banco o billetera");
		}
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
		banco = banco.trim();
	}

}
