package com.pasarela.comercios.dominio.puerto.entrada;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.util.UUID;

/**
 * Caso de uso: consultar un comercio. Un COMERCIO solo puede ver el suyo;
 * si pide otro, la respuesta es "no existe" (404) para no filtrar
 * existencia (HU-006).
 */
public interface ConsultarComercioUseCase {

	Comercio consultar(ComandoConsultarComercio comando);

	/** {@code comercioDelSolicitante} es null cuando consulta un ADMIN. */
	record ComandoConsultarComercio(UUID comercioId, UUID comercioDelSolicitante) {
	}

}
