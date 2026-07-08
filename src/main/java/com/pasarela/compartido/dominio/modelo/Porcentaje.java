package com.pasarela.compartido.dominio.modelo;

import com.pasarela.compartido.dominio.excepcion.PorcentajeInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Porcentaje entre 0 y 100 (ambos incluidos), inmutable.
 *
 * <p>El valor se normaliza a 4 decimales para que la escala no afecte la
 * igualdad: {@code Porcentaje.de("2.5")} y {@code Porcentaje.de("2.50")} son
 * el mismo porcentaje.</p>
 */
public record Porcentaje(BigDecimal valor) {

	private static final BigDecimal CIEN = new BigDecimal("100");
	private static final int ESCALA = 4;

	public Porcentaje {
		if (valor == null) {
			throw new PorcentajeInvalidoException("El porcentaje no puede ser nulo");
		}
		if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(CIEN) > 0) {
			throw new PorcentajeInvalidoException(
					"El porcentaje debe estar entre 0 y 100: " + valor.toPlainString());
		}
		valor = valor.setScale(ESCALA, RoundingMode.HALF_EVEN);
	}

	public static Porcentaje de(String valor) {
		if (valor == null) {
			throw new PorcentajeInvalidoException("El porcentaje no puede ser nulo");
		}
		try {
			return new Porcentaje(new BigDecimal(valor));
		} catch (NumberFormatException excepcion) {
			throw new PorcentajeInvalidoException("El porcentaje no es numérico: " + valor);
		}
	}

	/** Fracción decimal equivalente: 2.5% → 0.025. */
	public BigDecimal comoFraccion() {
		return valor.movePointLeft(2);
	}

}