package com.pasarela.pagos.dominio.puerto.salida;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto de salida: lo que el dominio necesita para persistir órdenes.
 * La infraestructura lo implementa (inversión de dependencias).
 */
public interface OrdenDePagoRepositorio {

	OrdenDePago guardar(OrdenDePago orden);

	Optional<OrdenDePago> buscarPorId(IdOrden id);

	/** Casa el webhook del proveedor con su orden. */
	Optional<OrdenDePago> buscarPorReferencia(ReferenciaPago referencia);

	/**
	 * Órdenes PENDIENTE_PAGO cuya ventana ya venció en el instante dado
	 * (insumo del job de expiración, HU-014), como máximo {@code limite}
	 * por llamada: el job procesa por lotes, jamás toda la tabla. El límite
	 * exacto NO cuenta como vencida — mismo criterio que
	 * {@code OrdenDePago.estaExpirada}.
	 */
	List<OrdenDePago> buscarPendientesExpiradas(Instant ahora, int limite);

	/**
	 * Órdenes PENDIENTE_PAGO creadas antes del instante dado — las
	 * "atascadas" que la reconciliación consulta activamente al proveedor
	 * (HU-015), como máximo {@code limite} por ciclo.
	 */
	List<OrdenDePago> buscarPendientesCreadasAntesDe(Instant limite, int maximo);

	/**
	 * Suma de las órdenes del comercio creadas en [desde, hasta) que
	 * consumen cupo mensual (HU-007/HU-008): las pendientes y las pagadas;
	 * las expiradas, fallidas y en revisión no cuentan.
	 */
	Dinero acumuladoDelMes(IdComercio comercioId, Instant desde, Instant hasta);

	/**
	 * Total y cantidad de ventas del comercio en [desde, hasta) contando
	 * SOLO los estados indicados (HU-018: la "venta efectiva" la define la
	 * aplicación — órdenes cuyo pago ya fue detectado o posterior).
	 */
	VentasTotalizadas totalizarVentas(IdComercio comercioId, Instant desde, Instant hasta,
			Set<EstadoOrden> estados);

	/**
	 * Órdenes del comercio creadas en [desde, hasta), de la más reciente a
	 * la más vieja, paginadas (HU-018): el listado incluye TODOS los
	 * estados — el comercio ve también sus pendientes y expiradas.
	 */
	PaginaDeOrdenes listarDelComercio(IdComercio comercioId, Instant desde, Instant hasta,
			int pagina, int tamano);

	record VentasTotalizadas(Dinero total, long cantidad) {
	}

	record PaginaDeOrdenes(List<OrdenDePago> ordenes, long totalElementos) {
	}

}