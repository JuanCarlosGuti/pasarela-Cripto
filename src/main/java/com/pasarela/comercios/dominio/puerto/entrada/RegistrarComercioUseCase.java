package com.pasarela.comercios.dominio.puerto.entrada;

import com.pasarela.comercios.dominio.modelo.Comercio;

/**
 * Caso de uso: el dueño de un negocio se registra para empezar el proceso
 * de verificación (HU-004). El comercio queda PENDIENTE y sin poder cobrar.
 */
public interface RegistrarComercioUseCase {

	Comercio registrar(ComandoRegistrarComercio comando);

	/** Datos crudos del registro; los VOs los construye y valida el dominio. */
	record ComandoRegistrarComercio(
			String razonSocial,
			String nit,
			String tipoCuenta,
			String numeroCuenta,
			String titularCuenta,
			String emailAcceso,
			String contrasenaAcceso) {
	}

}