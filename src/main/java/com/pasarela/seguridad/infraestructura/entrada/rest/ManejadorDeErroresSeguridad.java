package com.pasarela.seguridad.infraestructura.entrada.rest;

import com.pasarela.seguridad.dominio.excepcion.CredencialesInvalidasException;
import com.pasarela.seguridad.dominio.excepcion.UsuarioInvalidoException;
import com.pasarela.seguridad.dominio.excepcion.UsuarioYaExisteException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del contexto de seguridad a HTTP. El 401 de
 * credenciales es idéntico exista o no la cuenta (HU-006) y jamás se loguea
 * la contraseña.
 */
@RestControllerAdvice
public class ManejadorDeErroresSeguridad {

	public record ErrorResponse(String mensaje) {
	}

	@ExceptionHandler(CredencialesInvalidasException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse credencialesInvalidas(CredencialesInvalidasException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(UsuarioInvalidoException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse datosInvalidos(UsuarioInvalidoException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

	@ExceptionHandler(UsuarioYaExisteException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse cuentaDuplicada(UsuarioYaExisteException excepcion) {
		return new ErrorResponse(excepcion.getMessage());
	}

}
