package com.pasarela.comercios.dominio.puerto.entrada;

import com.pasarela.comercios.dominio.modelo.Comercio;

import java.util.List;

/**
 * Caso de uso: el Admin lista los comercios para trabajar la cola de
 * verificación (HU-026). Con estado, filtra; sin estado (null o vacío),
 * devuelve todos — siempre los más recientes primero.
 */
public interface ConsultarComerciosUseCase {

	List<Comercio> listar(String estado);

}
