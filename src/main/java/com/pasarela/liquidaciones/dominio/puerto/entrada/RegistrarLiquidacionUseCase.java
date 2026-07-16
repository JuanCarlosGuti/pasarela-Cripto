package com.pasarela.liquidaciones.dominio.puerto.entrada;

import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: el Admin registra la liquidación de un grupo de órdenes
 * CONVERTIDA (HU-016): calcula bruto/comisión/neto al centavo y las órdenes
 * pasan a LIQUIDADA. Una orden jamás pertenece a dos liquidaciones.
 *
 * <p>Desde HU-025, la conversión (tasa, comisión de rampa, referencia) ya
 * no la escribe el admin a mano — la resuelve el
 * {@code ProveedorDeRampaPort} (hoy siempre simulado; mañana el proveedor
 * real de T-007), igual que el pago lo resuelve {@code ProveedorDePagoPort}.</p>
 */
public interface RegistrarLiquidacionUseCase {

	Liquidacion registrar(ComandoRegistrarLiquidacion comando);

	record ComandoRegistrarLiquidacion(
			UUID comercioId,
			List<UUID> ordenes) {
	}

}
