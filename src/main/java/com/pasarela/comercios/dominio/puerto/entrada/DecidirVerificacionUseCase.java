package com.pasarela.comercios.dominio.puerto.entrada;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.util.UUID;

/**
 * Caso de uso: el Admin decide sobre la verificación de un comercio
 * (HU-005): APROBAR, RECHAZAR, SUSPENDER o REACTIVAR. Rechazo y suspensión
 * exigen motivo (auditable).
 */
public interface DecidirVerificacionUseCase {

	Comercio decidir(ComandoDecisionVerificacion comando);

	record ComandoDecisionVerificacion(UUID comercioId, String decision, String motivo) {
	}

}