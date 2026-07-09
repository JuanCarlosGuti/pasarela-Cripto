package com.pasarela.compartido.dominio.puerto;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;

/**
 * Puerto del kernel compartido: `pagos` pregunta si un comercio puede
 * recibir un cobro y `comercios` decide (estado de verificación + límites
 * de operación), sin que los contextos se importen entre sí.
 *
 * <p>El acumulado del mes lo calcula y aporta `pagos`, que es el dueño de
 * las órdenes; la regla de cumplimiento vive en `comercios`.</p>
 */
public interface AutorizadorDeCobros {

	/**
	 * Lanza {@code ComercioNoAutorizadoException} si el comercio no puede
	 * cobrar (no verificado/suspendido) o {@code LimiteExcedidoException}
	 * si el cobro viola sus topes — en ese caso el intento queda en la
	 * bitácora de operaciones inusuales.
	 */
	void autorizar(IdComercio comercioId, Dinero monto, Dinero acumuladoDelMes);

}
