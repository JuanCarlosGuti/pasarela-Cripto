package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.NitInvalidoException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NIT colombiano con su dígito de verificación validado según el algoritmo
 * oficial de la DIAN: cada dígito del NIT se multiplica (de derecha a
 * izquierda) por los pesos 3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59,
 * 67, 71; la suma se toma módulo 11. Si el residuo es 0 o 1, ese es el
 * dígito; en otro caso, el dígito es 11 − residuo.
 */
public record Nit(String numero, int digitoVerificacion) {

	private static final int[] PESOS_DIAN = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};
	private static final Pattern FORMATO = Pattern.compile("(\\d{5,15})-(\\d)");

	public Nit {
		if (numero == null || !numero.chars().allMatch(Character::isDigit)
				|| numero.length() < 5 || numero.length() > 15) {
			throw new NitInvalidoException(
					"El NIT debe tener entre 5 y 15 dígitos antes del guion");
		}
		int digitoEsperado = calcularDigitoDeVerificacion(numero);
		if (digitoVerificacion != digitoEsperado) {
			throw new NitInvalidoException(
					"El dígito de verificación del NIT %s no corresponde (se esperaba %d)"
							.formatted(numero, digitoEsperado));
		}
	}

	/** Crea el NIT desde el formato usual {@code "899999068-1"}. */
	public static Nit de(String texto) {
		if (texto == null || texto.isBlank()) {
			throw new NitInvalidoException("El NIT no puede estar vacío");
		}
		Matcher matcher = FORMATO.matcher(texto.trim());
		if (!matcher.matches()) {
			throw new NitInvalidoException(
					"El NIT debe tener el formato número-dígito, por ejemplo 899999068-1");
		}
		return new Nit(matcher.group(1), Integer.parseInt(matcher.group(2)));
	}

	public String completo() {
		return numero + "-" + digitoVerificacion;
	}

	private static int calcularDigitoDeVerificacion(String numero) {
		int suma = 0;
		for (int i = 0; i < numero.length(); i++) {
			int digito = numero.charAt(numero.length() - 1 - i) - '0';
			suma += digito * PESOS_DIAN[i];
		}
		int residuo = suma % 11;
		return residuo <= 1 ? residuo : 11 - residuo;
	}

}