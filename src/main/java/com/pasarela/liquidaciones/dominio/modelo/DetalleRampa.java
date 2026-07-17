package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;

import java.math.BigDecimal;

/**
 * Detalle de la conversión de la rampa (HU-025, ADR-006): lo que el
 * proveedor de rampa cobró y la tasa a la que convirtió. Hoy siempre viene
 * de {@code ProveedorDeRampaSimulado}; con el proveedor real (T-007) el
 * origen cambia, la forma no.
 */
public record DetalleRampa(
		Dinero comisionRampa, BigDecimal tasaCambioSimulada, String cuentaDestinoDescripcion) {

	public DetalleRampa {
		if (comisionRampa == null) {
			throw new LiquidacionInvalidaException(
					"El detalle de la rampa requiere la comisión cobrada");
		}
		if (tasaCambioSimulada == null || tasaCambioSimulada.signum() <= 0) {
			throw new LiquidacionInvalidaException(
					"El detalle de la rampa requiere una tasa de cambio positiva");
		}
		if (cuentaDestinoDescripcion == null || cuentaDestinoDescripcion.isBlank()) {
			throw new LiquidacionInvalidaException(
					"El detalle de la rampa requiere la cuenta destino");
		}
	}

}
