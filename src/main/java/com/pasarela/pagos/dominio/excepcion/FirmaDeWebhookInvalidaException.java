package com.pasarela.pagos.dominio.excepcion;

/**
 * La firma del webhook no corresponde al proveedor: el intento queda
 * registrado y se responde 401 sin tocar ninguna orden (HU-012).
 */
public class FirmaDeWebhookInvalidaException extends RuntimeException {

	public FirmaDeWebhookInvalidaException(String mensaje) {
		super(mensaje);
	}

}
