package com.pasarela.pagos.dominio.puerto.salida;

import com.pasarela.pagos.dominio.modelo.EventoProveedor;

/**
 * Puerto de salida: persistencia de los eventos crudos del proveedor.
 * La unicidad (proveedor, idExternoEvento) vive como constraint en BD:
 * es la última línea de defensa de la idempotencia (ADR-004).
 */
public interface EventoProveedorRepositorio {

	/**
	 * Persiste el evento en TRANSACCIÓN PROPIA: el crudo debe sobrevivir
	 * aunque el procesamiento posterior falle o se rechace.
	 *
	 * @throws EventoDuplicadoException si ya existe (proveedor, idExterno).
	 */
	EventoProveedor guardar(EventoProveedor evento);

	boolean existe(String proveedor, String idExternoEvento);

	/** Señal de que la constraint de unicidad detuvo un evento repetido. */
	class EventoDuplicadoException extends RuntimeException {
		public EventoDuplicadoException(String mensaje) {
			super(mensaje);
		}
	}

}
