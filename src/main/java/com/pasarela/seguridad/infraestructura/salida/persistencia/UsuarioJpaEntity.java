package com.pasarela.seguridad.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Representación JPA de la cuenta de usuario, separada del dominio. */
@Entity
@Table(name = "usuarios")
public class UsuarioJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 150)
	private String email;

	@Column(name = "hash_contrasena", nullable = false, length = 100)
	private String hashContrasena;

	@Column(nullable = false, length = 20)
	private String rol;

	@Column(name = "comercio_id")
	private UUID comercioId;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected UsuarioJpaEntity() {
		// requerido por JPA
	}

	UsuarioJpaEntity(UUID id, String email, String hashContrasena, String rol,
			UUID comercioId, Instant creadoEn) {
		this.id = id;
		this.email = email;
		this.hashContrasena = hashContrasena;
		this.rol = rol;
		this.comercioId = comercioId;
		this.creadoEn = creadoEn;
	}

	UUID id() {
		return id;
	}

	String email() {
		return email;
	}

	String hashContrasena() {
		return hashContrasena;
	}

	String rol() {
		return rol;
	}

	UUID comercioId() {
		return comercioId;
	}

	Instant creadoEn() {
		return creadoEn;
	}

}
