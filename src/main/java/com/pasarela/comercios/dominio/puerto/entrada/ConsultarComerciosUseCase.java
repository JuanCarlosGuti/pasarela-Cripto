package com.pasarela.comercios.dominio.puerto.entrada;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.util.List;

/**
 * Caso de uso: el Admin lista los comercios para trabajar la cola de
 * verificación (HU-026; paginado desde HU-027 — la cola crece y una lista
 * infinita no es una cola). Con estado, filtra; sin estado (null o vacío),
 * devuelve todos — siempre los más recientes primero.
 */
public interface ConsultarComerciosUseCase {

	PaginaDeComercios listar(String estado, int pagina, int tamano);

	record PaginaDeComercios(
			List<Comercio> comercios, long totalElementos, int pagina, int tamano) {
	}

}
