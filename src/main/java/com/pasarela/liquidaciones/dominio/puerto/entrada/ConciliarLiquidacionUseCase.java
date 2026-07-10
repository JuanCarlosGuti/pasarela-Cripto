package com.pasarela.liquidaciones.dominio.puerto.entrada;

import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: conciliar una liquidación contra lo reportado por el
 * proveedor (HU-017). Coincide → CONCILIADA; cualquier diferencia →
 * DISCREPANCIA con detalle y alerta al Admin (bitácora). Jamás se cuadra
 * en silencio.
 */
public interface ConciliarLiquidacionUseCase {

	Liquidacion conciliar(ComandoConciliar comando);

	/** Lo reportado por el proveedor; montos en pesos (COP). */
	record ComandoConciliar(
			UUID liquidacionId,
			long montoBrutoReportado,
			List<UUID> ordenesReportadas,
			String actor) {
	}

}
