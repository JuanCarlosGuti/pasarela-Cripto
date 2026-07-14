package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.compartido.dominio.excepcion.MontoInvalidoException;
import com.pasarela.pagos.dominio.excepcion.ComprobanteNoDisponibleException;
import com.pasarela.pagos.dominio.excepcion.FirmaDeWebhookInvalidaException;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.excepcion.OrdenNoEncontradaException;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones del contexto de pagos a HTTP. */
@RestControllerAdvice
public class ManejadorDeErroresPagos {

	public record ErrorResponse(String mensaje) {
	}

	@ExceptionHandler({OrdenInvalidaException.class, MontoInvalidoException.class,
			WebhookInvalidoException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse datosInvalidos(RuntimeException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(FirmaDeWebhookInvalidaException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse firmaInvalida(FirmaDeWebhookInvalidaException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(OrdenNoEncontradaException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse noEncontrada(OrdenNoEncontradaException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	/** La orden existe pero su estado no da para comprobante (HU-020): 422. */
	@ExceptionHandler(ComprobanteNoDisponibleException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public ErrorResponse comprobanteNoDisponible(ComprobanteNoDisponibleException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(ProveedorDePagoNoDisponibleException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public ErrorResponse proveedorNoDisponible(ProveedorDePagoNoDisponibleException excepcion) {
		return new ErrorResponse(
				"No fue posible registrar el cobro con el proveedor; intenta de nuevo");
	}

}
