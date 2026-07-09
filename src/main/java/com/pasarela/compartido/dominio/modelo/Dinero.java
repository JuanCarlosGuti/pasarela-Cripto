package com.pasarela.compartido.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.MonedasDistintasException;
import com.pasarela.compartido.dominio.excepcion.MontoInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cantidad de dinero en una moneda concreta, inmutable y nunca negativa.
 *
 * <p>Invariantes:</p>
 * <ul>
 *   <li>El monto se normaliza a los decimales de la {@link Moneda} con
 *       redondeo bancario ({@link RoundingMode#HALF_EVEN}), de modo que la
 *       escala no afecta la igualdad (1000 COP == 1000.00 COP).</li>
 *   <li>No existe el dinero negativo: cualquier operación cuyo resultado sea
 *       menor que cero lanza {@link MontoInvalidoException}.</li>
 *   <li>Solo se opera entre montos de la misma moneda.</li>
 * </ul>
 */
public record Dinero(BigDecimal monto, Moneda moneda) {

	public Dinero {
		if (monto == null) {
			throw new MontoInvalidoException("El monto no puede ser nulo");
		}
		if (moneda == null) {
			throw new MontoInvalidoException("La moneda no puede ser nula");
		}
		if (monto.signum() < 0) {
			throw new MontoInvalidoException(
					"El monto no puede ser negativo: " + monto.toPlainString());
		}
		monto = monto.setScale(moneda.decimales(), RoundingMode.HALF_EVEN);
	}

	public static Dinero de(BigDecimal monto, Moneda moneda) {
		return new Dinero(monto, moneda);
	}

	public static Dinero cop(long monto) {
		return new Dinero(BigDecimal.valueOf(monto), Moneda.COP);
	}

	public Dinero sumar(Dinero otro) {
		validarMismaMoneda(otro);
		return new Dinero(monto.add(otro.monto), moneda);
	}

	public Dinero restar(Dinero otro) {
		validarMismaMoneda(otro);
		return new Dinero(monto.subtract(otro.monto), moneda);
	}

	public Dinero porcentaje(Porcentaje porcentaje) {
		return new Dinero(monto.multiply(porcentaje.comoFraccion()), moneda);
	}

	public boolean esCero() {
		return monto.signum() == 0;
	}

	public boolean esMayorQue(Dinero otro) {
		validarMismaMoneda(otro);
		return monto.compareTo(otro.monto) > 0;
	}

	private void validarMismaMoneda(Dinero otro) {
		if (moneda != otro.moneda) {
			throw new MonedasDistintasException(
					"No se puede operar entre monedas distintas: %s y %s"
							.formatted(moneda, otro.moneda));
		}
	}

}