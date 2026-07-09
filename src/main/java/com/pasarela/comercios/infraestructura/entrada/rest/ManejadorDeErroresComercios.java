package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.ComercioYaRegistradoException;
import com.pasarela.comercios.dominio.excepcion.NitInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del contexto de comercios a HTTP. Los mensajes son
 * accionables pero nunca incluyen datos sensibles (cuentas, tokens).
 */
@RestControllerAdvice
public class ManejadorDeErroresComercios {

	public record ErrorResponse(String mensaje) {
	}

	@ExceptionHandler({NitInvalidoException.class, ComercioInvalidoException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse datosInvalidos(RuntimeException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(ComercioYaRegistradoException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse duplicado(ComercioYaRegistradoException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse cuerpoInvalido(MethodArgumentNotValidException excepcion) {
		String detalle = excepcion.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.reduce((a, b) -> a + "; " + b)
				.orElse("Cuerpo de la petición inválido");
		return new ErrorResponse(detalle);
	}

}