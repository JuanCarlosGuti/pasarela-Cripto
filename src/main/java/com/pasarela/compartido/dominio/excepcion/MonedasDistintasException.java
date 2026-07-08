package com.pasarela.compartido.dominio.excepcion;

public class MonedasDistintasException extends RuntimeException {

	public MonedasDistintasException(String mensaje) {
		super(mensaje);
	}

}
