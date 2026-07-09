package com.pasarela.seguridad.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.seguridad.dominio.excepcion.UsuarioInvalidoException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Cuenta de acceso a la plataforma. El dominio guarda solo el HASH de la
 * contraseña (BCrypt, vía puerto): la contraseña en claro jamás se persiste
 * ni se loguea.
 *
 * <p>Invariante: un usuario COMERCIO siempre está asociado a un comercio;
 * un ADMIN nunca.</p>
 */
public class Usuario {

	private final IdUsuario id;
	private final String email;
	private final String hashContrasena;
	private final RolUsuario rol;
	private final IdComercio comercioId;
	private final Instant creadoEn;

	private Usuario(IdUsuario id, String email, String hashContrasena,
			RolUsuario rol, IdComercio comercioId, Instant creadoEn) {
		this.id = id;
		this.email = email;
		this.hashContrasena = hashContrasena;
		this.rol = rol;
		this.comercioId = comercioId;
		this.creadoEn = creadoEn;
	}

	public static Usuario crearAdmin(String email, String hashContrasena, Instant ahora) {
		return new Usuario(IdUsuario.generar(), validarEmail(email),
				validarHash(hashContrasena), RolUsuario.ADMIN, null,
				validarFecha(ahora));
	}

	public static Usuario crearComercio(String email, String hashContrasena,
			IdComercio comercioId, Instant ahora) {
		return new Usuario(IdUsuario.generar(), validarEmail(email),
				validarHash(hashContrasena), RolUsuario.COMERCIO,
				validarComercio(RolUsuario.COMERCIO, comercioId), validarFecha(ahora));
	}

	/** Rehidratación desde persistencia. */
	public static Usuario reconstituir(IdUsuario id, String email, String hashContrasena,
			RolUsuario rol, IdComercio comercioId, Instant creadoEn) {
		if (id == null || rol == null) {
			throw new UsuarioInvalidoException("El usuario requiere id y rol");
		}
		return new Usuario(id, validarEmail(email), validarHash(hashContrasena),
				rol, validarComercio(rol, comercioId), validarFecha(creadoEn));
	}

	private static String validarEmail(String email) {
		if (email == null || email.isBlank() || !email.contains("@")) {
			throw new UsuarioInvalidoException("El email de la cuenta no es válido");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String validarHash(String hashContrasena) {
		if (hashContrasena == null || hashContrasena.isBlank()) {
			throw new UsuarioInvalidoException(
					"La cuenta requiere el hash de la contraseña");
		}
		return hashContrasena;
	}

	private static Instant validarFecha(Instant instante) {
		if (instante == null) {
			throw new UsuarioInvalidoException("La fecha de creación es obligatoria");
		}
		return instante;
	}

	private static IdComercio validarComercio(RolUsuario rol, IdComercio comercioId) {
		if (rol == RolUsuario.COMERCIO && comercioId == null) {
			throw new UsuarioInvalidoException(
					"Un usuario COMERCIO requiere el comercio asociado");
		}
		if (rol == RolUsuario.ADMIN && comercioId != null) {
			throw new UsuarioInvalidoException(
					"Un usuario ADMIN no puede tener comercio asociado");
		}
		return comercioId;
	}

	public IdUsuario id() {
		return id;
	}

	public String email() {
		return email;
	}

	public String hashContrasena() {
		return hashContrasena;
	}

	public RolUsuario rol() {
		return rol;
	}

	/** Comercio asociado; null para ADMIN. */
	public IdComercio comercioId() {
		return comercioId;
	}

	public Instant creadoEn() {
		return creadoEn;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof Usuario otroUsuario && id.equals(otroUsuario.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
