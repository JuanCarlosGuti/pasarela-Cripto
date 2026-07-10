package com.pasarela.pagos.dominio.puerto.entrada;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso del dashboard (HU-018): el comercio ve sus ventas del día y
 * del mes, y lista su historial paginado.
 *
 * <p><b>Definición de venta efectiva:</b> una orden cuenta como venta si su
 * pago ya fue detectado o posterior (PAGO_DETECTADO, CONVERTIDA o
 * LIQUIDADA). Las pendientes, expiradas, fallidas y en revisión NO suman.
 * Los cortes de día y mes son calendario en zona America/Bogota.</p>
 */
public interface ConsultarVentasUseCase {

	/** Ventas del día de hoy y del mes en curso del comercio. */
	ResumenDeVentas resumen(UUID comercioId);

	/** Historial paginado del comercio (TODOS los estados) en [desde, hasta]. */
	PaginaDeVentas listar(ConsultaDeVentas consulta);

	record ConsultaDeVentas(UUID comercioId, LocalDate desde, LocalDate hasta,
			int pagina, int tamano) {
	}

	record TotalDeVentas(Dinero total, long cantidad) {
	}

	record ResumenDeVentas(TotalDeVentas dia, TotalDeVentas mes) {
	}

	record PaginaDeVentas(List<OrdenDePago> ordenes, long totalElementos,
			int pagina, int tamano) {
	}

}
