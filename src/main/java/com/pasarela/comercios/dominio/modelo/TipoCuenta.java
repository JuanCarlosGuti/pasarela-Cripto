package com.pasarela.comercios.dominio.modelo;

/**
 * Tipo de la cuenta donde el proveedor liquida los COP. El banco/billetera
 * vive aparte en {@link CuentaLiquidacion#banco()} (HU-027): las billeteras
 * (Nequi, Daviplata...) se manejan internamente como AHORROS.
 */
public enum TipoCuenta {

	AHORROS,
	CORRIENTE

}
