package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;

import java.util.List;

/**
 * Lo que el proveedor reporta haber liquidado (HU-017): el monto bruto y
 * las órdenes incluidas. Se compara contra lo registrado — jamás se
 * "cuadra" en silencio.
 */
public record ReporteDelProveedor(Dinero montoBruto, List<IdOrden> ordenes) {

	public ReporteDelProveedor {
		if (montoBruto == null) {
			throw new LiquidacionInvalidaException(
					"El reporte del proveedor requiere el monto bruto");
		}
		if (ordenes == null || ordenes.isEmpty()) {
			throw new LiquidacionInvalidaException(
					"El reporte del proveedor requiere las órdenes incluidas");
		}
		ordenes = List.copyOf(ordenes);
	}

}
