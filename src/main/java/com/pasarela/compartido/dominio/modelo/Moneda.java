package com.pasarela.compartido.dominio.modelo;

/**
 * Monedas soportadas por la plataforma, cada una con su cantidad de decimales
 * significativos: los montos en {@link Dinero} se normalizan a esa escala.
 */
public enum Moneda {

	/** Peso colombiano: sin decimales en la práctica comercial. */
	COP(0),

	/** Tether (USDT): 6 decimales, la precisión estándar del token. */
	USDT(6);

	private final int decimales;

	Moneda(int decimales) {
		this.decimales = decimales;
	}

	public int decimales() {
		return decimales;
	}

}