package com.pasarela.seguridad.dominio.puerto.entrada;

import com.pasarela.seguridad.dominio.modelo.Usuario;

/**
 * Caso de uso: autenticar credenciales (HU-006). Devuelve el usuario si las
 * credenciales son válidas; en cualquier otro caso lanza
 * {@code CredencialesInvalidasException} sin revelar la causa.
 */
public interface AutenticarUsuarioUseCase {

	Usuario autenticar(ComandoAutenticar comando);

	record ComandoAutenticar(String usuario, String contrasena) {
	}

}
