package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.pagos.dominio.excepcion.ComisionInvalidaException;

/**
 * Comisión de la plataforma sobre un cobro: el ingreso del negocio.
 *
 * <p>El redondeo es el de {@link Dinero}: a los decimales de la moneda con
 * redondeo bancario (HALF_EVEN). El neto se define por diferencia
 * ({@code neto = monto − comisión}), de modo que comisión + neto suman
 * exactamente el monto original: nunca se pierde ni se crea un centavo.</p>
 */
public record Comision(Porcentaje tasa) {

	public Comision {
		if (tasa == null) {
			throw new ComisionInvalidaException("La tasa de la comisión no puede ser nula");
		}
	}

	/** Monto de la comisión de la plataforma sobre el cobro. */
	public Dinero calcular(Dinero monto) {
		return monto.porcentaje(tasa);
	}

	/** Lo que recibe el comercio: el monto menos la comisión. */
	public Dinero netoParaElComercio(Dinero monto) {
		return monto.restar(calcular(monto));
	}

}