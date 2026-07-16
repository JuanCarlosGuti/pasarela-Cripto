package com.pasarela.compartido.dominio.puerto;

import com.pasarela.compartido.dominio.modelo.IdComercio;

/**
 * Puerto del kernel compartido (HU-025): `liquidaciones` necesita la cuenta
 * donde el comercio recibe los COP, pero esa cuenta vive en `comercios`, sin
 * que los contextos se importen entre sí.
 */
public interface ConsultorDeCuentaLiquidacion {

	DatosCuentaLiquidacion obtener(IdComercio comercioId);

	record DatosCuentaLiquidacion(String tipoCuenta, String numero, String titular) {
	}

}
