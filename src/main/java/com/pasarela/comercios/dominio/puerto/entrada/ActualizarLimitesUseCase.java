package com.pasarela.comercios.dominio.puerto.entrada;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.util.UUID;

/**
 * Caso de uso: el Admin ajusta los topes de operación de un comercio
 * (HU-007). El cambio queda auditado en la bitácora: quién, cuándo y
 * valores anterior/nuevo. Montos en pesos (COP, sin decimales).
 */
public interface ActualizarLimitesUseCase {

	Comercio actualizar(ComandoActualizarLimites comando);

	record ComandoActualizarLimites(
			UUID comercioId,
			long topePorTransaccion,
			long topeMensual,
			String actor) {
	}

}
