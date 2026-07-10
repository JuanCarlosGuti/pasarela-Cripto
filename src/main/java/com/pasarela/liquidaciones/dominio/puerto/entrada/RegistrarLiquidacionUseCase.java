package com.pasarela.liquidaciones.dominio.puerto.entrada;

import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: el Admin registra la liquidación que el proveedor hizo al
 * comercio (HU-016): agrupa órdenes CONVERTIDA, calcula bruto/comisión/neto
 * al centavo y las órdenes pasan a LIQUIDADA. Una orden jamás pertenece a
 * dos liquidaciones.
 */
public interface RegistrarLiquidacionUseCase {

	Liquidacion registrar(ComandoRegistrarLiquidacion comando);

	record ComandoRegistrarLiquidacion(
			UUID comercioId,
			List<UUID> ordenes,
			String referenciaProveedor) {
	}

}
