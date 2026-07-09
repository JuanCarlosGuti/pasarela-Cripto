package com.pasarela.seguridad.dominio.puerto.salida;

import com.pasarela.seguridad.dominio.modelo.Usuario;

import java.util.Optional;

/** Puerto de salida: persistencia de cuentas de usuario. */
public interface UsuarioRepositorio {

	Usuario guardar(Usuario usuario);

	Optional<Usuario> buscarPorEmail(String email);

}
