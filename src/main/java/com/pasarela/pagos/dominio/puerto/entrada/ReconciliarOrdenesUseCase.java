package com.pasarela.pagos.dominio.puerto.entrada;

/**
 * Caso de uso del job de reconciliación (HU-015, cierra el ADR-004): las
 * órdenes PENDIENTE_PAGO atascadas se consultan activamente al proveedor;
 * si reporta el pago, se confirman por la MISMA ruta idempotente del
 * webhook. El sistema converge al estado correcto aunque un webhook jamás
 * llegue. Un proveedor caído no tumba el ciclo: se registra y se reintenta
 * en el siguiente.
 */
public interface ReconciliarOrdenesUseCase {

	ResultadoReconciliacion reconciliar();

	record ResultadoReconciliacion(int consultadas, int confirmadas, int fallosDelProveedor) {
	}

}
