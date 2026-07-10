package com.pasarela.compartido.dominio.puerto;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;

import java.time.Instant;
import java.util.List;

/**
 * Puerto del kernel compartido (HU-016): `liquidaciones` agrupa órdenes que
 * pertenecen a `pagos`, sin que los contextos se importen entre sí. `pagos`
 * valida y transiciona sus órdenes; `liquidaciones` calcula y registra el
 * dinero.
 */
public interface LiquidadorDeOrdenes {

	/**
	 * Valida que todas las órdenes existan, sean del comercio y estén
	 * CONVERTIDA, y devuelve sus montos. Lanza
	 * {@code OrdenNoLiquidableException} si alguna no cumple.
	 */
	List<OrdenLiquidable> obtenerConvertidas(IdComercio comercioId, List<IdOrden> ordenes);

	/** CONVERTIDA → LIQUIDADA para cada orden (máquina de estados de pagos). */
	void marcarComoLiquidadas(List<IdOrden> ordenes, Instant ahora);

	record OrdenLiquidable(IdOrden id, Dinero monto) {
	}

}
