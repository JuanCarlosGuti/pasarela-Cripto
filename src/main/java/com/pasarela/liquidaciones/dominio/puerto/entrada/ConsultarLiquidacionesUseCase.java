package com.pasarela.liquidaciones.dominio.puerto.entrada;

import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;

import java.util.List;
import java.util.UUID;

/** Caso de uso: el comercio consulta SUS liquidaciones (HU-025). */
public interface ConsultarLiquidacionesUseCase {

	List<Liquidacion> listar(UUID comercioId);

}
