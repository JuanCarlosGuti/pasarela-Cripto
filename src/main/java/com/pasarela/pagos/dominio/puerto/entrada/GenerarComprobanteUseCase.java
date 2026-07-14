package com.pasarela.pagos.dominio.puerto.entrada;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso: emitir el comprobante de una venta (HU-020) — el soporte que
 * el comercio muestra al cliente y entrega al contador. Solo existe para
 * órdenes con el pago detectado o posterior; pedirlo para una no pagada es
 * {@code ComprobanteNoDisponibleException} (422). Mismo aislamiento que la
 * consulta de la orden: una ajena responde igual que una inexistente.
 */
public interface GenerarComprobanteUseCase {

	Comprobante generar(ComandoGenerarComprobante comando);

	/** {@code comercioDelSolicitante} es null cuando lo pide un ADMIN. */
	record ComandoGenerarComprobante(UUID ordenId, UUID comercioDelSolicitante) {
	}

	/**
	 * Derivado inmutable de la orden: {@code numero} es el id de la orden
	 * (pedirlo dos veces da el mismo comprobante) y {@code referencia} es la
	 * referencia del cobro ante el proveedor — la misma que viaja en el QR y
	 * en el webhook. {@code liquidadaEn} es null mientras el proveedor no
	 * haya liquidado los COP al comercio.
	 */
	record Comprobante(
			UUID numero,
			String referencia,
			Dinero monto,
			EstadoOrden estado,
			Instant creadaEn,
			Instant pagoDetectadoEn,
			Instant liquidadaEn,
			Instant emitidoEn) {
	}

}
