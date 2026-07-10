package com.pasarela.pagos.dominio.excepcion;

/** El webhook o el evento del proveedor traen datos inválidos o incompletos. */
public class WebhookInvalidoException extends RuntimeException {

	public WebhookInvalidoException(String mensaje) {
		super(mensaje);
	}

	public WebhookInvalidoException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

}
