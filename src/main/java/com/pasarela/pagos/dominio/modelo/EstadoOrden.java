package com.pasarela.pagos.dominio.modelo;

/**
 * Estados del ciclo de vida de una orden de pago. La matriz de transiciones
 * válidas (docs/04-modelo-de-dominio.md) vive aquí, en un solo lugar: todo lo
 * que {@link #puedeTransicionarA(EstadoOrden)} no permita, se rechaza.
 */
public enum EstadoOrden {

	CREADA,
	PENDIENTE_PAGO,
	PAGO_DETECTADO,
	CONVERTIDA,
	LIQUIDADA,
	EXPIRADA,
	FALLIDA,
	EN_REVISION;

	public boolean puedeTransicionarA(EstadoOrden destino) {
		return switch (this) {
			case CREADA -> destino == PENDIENTE_PAGO;
			// FALLIDA desde pendiente: pago inválido detectado, p. ej. monto errado (HU-012)
			case PENDIENTE_PAGO -> destino == PAGO_DETECTADO || destino == EXPIRADA
					|| destino == FALLIDA;
			case PAGO_DETECTADO -> destino == CONVERTIDA || destino == FALLIDA;
			case CONVERTIDA -> destino == LIQUIDADA;
			case FALLIDA -> destino == EN_REVISION;
			case LIQUIDADA, EXPIRADA, EN_REVISION -> false;
		};
	}

	/** Terminal: la orden ya no admite ninguna transición. */
	public boolean esTerminal() {
		return this == LIQUIDADA || this == EXPIRADA || this == EN_REVISION;
	}

	/**
	 * Venta efectiva: el pago ya fue detectado o va más allá. Es la definición
	 * que suman los totales del dashboard (HU-018) y la condición para emitir
	 * comprobante (HU-020); pendientes, expiradas, fallidas y en revisión no.
	 */
	public boolean esVentaEfectiva() {
		return this == PAGO_DETECTADO || this == CONVERTIDA || this == LIQUIDADA;
	}

}