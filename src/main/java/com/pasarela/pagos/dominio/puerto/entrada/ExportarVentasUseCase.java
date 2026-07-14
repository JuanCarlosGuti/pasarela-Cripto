package com.pasarela.pagos.dominio.puerto.entrada;

import com.pasarela.compartido.dominio.modelo.Dinero;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: exportar el historial de movimientos del comercio (HU-019).
 * Incluye órdenes en CUALQUIER estado (es el historial completo, no solo
 * ventas efectivas — a diferencia de {@link ConsultarVentasUseCase#resumen}).
 *
 * <p>La comisión y el neto se calculan por fila con la tasa vigente de la
 * plataforma (la misma que usa el registro de liquidaciones): no hay un
 * valor de comisión guardado por orden individual, solo por lote liquidado,
 * así que esta es la cifra que produciría liquidar esa orden sola hoy.</p>
 */
public interface ExportarVentasUseCase {

	List<FilaMovimiento> exportar(ComandoExportarVentas comando);

	record ComandoExportarVentas(UUID comercioId, LocalDate desde, LocalDate hasta) {
	}

	record FilaMovimiento(Instant fecha, String referencia, Dinero montoBruto,
			Dinero comision, Dinero neto, String estado) {
	}

}
