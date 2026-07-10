package com.pasarela.liquidaciones.dominio.modelo;

/**
 * Estado de la liquidación (docs/04): REGISTRADA al nacer; la conciliación
 * (HU-017) la pasa a CONCILIADA o a DISCREPANCIA — jamás se "cuadra" en
 * silencio.
 */
public enum EstadoLiquidacion {

	REGISTRADA,
	CONCILIADA,
	DISCREPANCIA

}
