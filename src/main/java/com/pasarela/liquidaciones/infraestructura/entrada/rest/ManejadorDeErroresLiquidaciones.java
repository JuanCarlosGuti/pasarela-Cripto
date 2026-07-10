package com.pasarela.liquidaciones.infraestructura.entrada.rest;

import com.pasarela.compartido.dominio.excepcion.OrdenNoLiquidableException;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;
import com.pasarela.liquidaciones.dominio.excepcion.OrdenYaLiquidadaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones del contexto de liquidaciones a HTTP. */
@RestControllerAdvice
public class ManejadorDeErroresLiquidaciones {

	public record ErrorResponse(String mensaje) {
	}

	@ExceptionHandler(LiquidacionInvalidaException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse datosInvalidos(LiquidacionInvalidaException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(OrdenNoLiquidableException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public ErrorResponse ordenNoLiquidable(OrdenNoLiquidableException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(OrdenYaLiquidadaException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse ordenYaLiquidada(OrdenYaLiquidadaException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

}
