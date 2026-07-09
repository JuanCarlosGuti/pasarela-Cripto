package com.pasarela.comercios.dominio.modelo;

/**
 * Estado de la verificación manual del comercio (HU-005 define las
 * transiciones; en el registro el comercio siempre nace PENDIENTE).
 */
public enum EstadoVerificacion {

	PENDIENTE,
	VERIFICADO,
	RECHAZADO,
	SUSPENDIDO

}