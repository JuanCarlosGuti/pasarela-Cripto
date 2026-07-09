package com.pasarela.seguridad.aplicacion;

import com.pasarela.seguridad.dominio.excepcion.CredencialesInvalidasException;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.entrada.AutenticarUsuarioUseCase;
import com.pasarela.seguridad.dominio.puerto.salida.HasheadorDeContrasena;
import com.pasarela.seguridad.dominio.puerto.salida.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Autenticación de credenciales (HU-006). Toda falla produce EXACTAMENTE el
 * mismo error, y cuando el usuario no existe se compara igual contra un hash
 * señuelo: ni el mensaje ni el tiempo de respuesta revelan si la cuenta
 * existe.
 */
@Service
public class AutenticarUsuarioService implements AutenticarUsuarioUseCase {

	/** Hash BCrypt de un valor aleatorio descartado; solo iguala el costo de cómputo. */
	private static final String HASH_SENUELO =
			"$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5B0aG0P3xN9d0V4uW1v6mJXlZKfHe";

	private final UsuarioRepositorio repositorio;
	private final HasheadorDeContrasena hasheador;

	public AutenticarUsuarioService(UsuarioRepositorio repositorio,
			HasheadorDeContrasena hasheador) {
		this.repositorio = repositorio;
		this.hasheador = hasheador;
	}

	@Override
	@Transactional(readOnly = true)
	public Usuario autenticar(ComandoAutenticar comando) {
		if (comando.usuario() == null || comando.usuario().isBlank()
				|| comando.contrasena() == null || comando.contrasena().isBlank()) {
			throw new CredencialesInvalidasException();
		}
		Optional<Usuario> usuario = repositorio.buscarPorEmail(
				comando.usuario().trim().toLowerCase(Locale.ROOT));
		String hash = usuario.map(Usuario::hashContrasena).orElse(HASH_SENUELO);
		if (!hasheador.coincide(comando.contrasena(), hash) || usuario.isEmpty()) {
			throw new CredencialesInvalidasException();
		}
		return usuario.get();
	}

}
