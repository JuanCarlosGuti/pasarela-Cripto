package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.LimiteExcedidoException;
import com.pasarela.compartido.dominio.modelo.Dinero;

/**
 * Topes de operación del comercio (HU-007, cumplimiento desde el MVP):
 * por transacción y acumulado por mes. Llegar exactamente al tope es
 * válido; superarlo por un peso se rechaza.
 */
public record LimitesOperacion(Dinero topePorTransaccion, Dinero topeMensual) {

	public LimitesOperacion {
		if (topePorTransaccion == null || topeMensual == null) {
			throw new ComercioInvalidoException(
					"Los límites de operación requieren ambos topes");
		}
		if (topePorTransaccion.esCero() || topeMensual.esCero()) {
			throw new ComercioInvalidoException(
					"Los topes de operación deben ser mayores que cero");
		}
		if (topePorTransaccion.esMayorQue(topeMensual)) {
			throw new ComercioInvalidoException(
					"El tope por transacción no puede superar el tope mensual");
		}
	}

	/** Topes iniciales del MVP; el Admin los ajusta por comercio. */
	public static LimitesOperacion porDefecto() {
		return new LimitesOperacion(Dinero.cop(2_000_000), Dinero.cop(20_000_000));
	}

	/**
	 * Regla de cumplimiento: valida un cobro contra ambos topes dado lo ya
	 * acumulado en el mes. Lanza {@link LimiteExcedidoException} con el tope
	 * violado y los valores (para la bitácora).
	 */
	public void validarCobro(Dinero monto, Dinero acumuladoDelMes) {
		if (monto == null || acumuladoDelMes == null) {
			throw new ComercioInvalidoException(
					"Validar límites requiere el monto y el acumulado del mes");
		}
		if (monto.esMayorQue(topePorTransaccion)) {
			throw new LimiteExcedidoException(
					"El cobro de %s supera el tope por transacción de %s"
							.formatted(enPesos(monto), enPesos(topePorTransaccion)));
		}
		if (acumuladoDelMes.sumar(monto).esMayorQue(topeMensual)) {
			throw new LimiteExcedidoException(
					"El cobro de %s excedería el tope mensual de %s (acumulado: %s)"
							.formatted(enPesos(monto), enPesos(topeMensual),
									enPesos(acumuladoDelMes)));
		}
	}

	private static String enPesos(Dinero dinero) {
		return dinero.monto().toPlainString() + " " + dinero.moneda();
	}

}
