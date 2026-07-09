package com.pasarela.pagos.dominio.puerto.entrada;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;

import java.util.UUID;

/**
 * Caso de uso: el dueño consulta una de sus órdenes con el detalle completo
 * (estado, monto, timestamps de transición). Pedir la orden de otro
 * comercio responde igual que una inexistente (HU-009).
 */
public interface ConsultarOrdenUseCase {

	OrdenDePago consultar(ComandoConsultarOrden comando);

	/** {@code comercioDelSolicitante} es null cuando consulta un ADMIN. */
	record ComandoConsultarOrden(UUID ordenId, UUID comercioDelSolicitante) {
	}

}
