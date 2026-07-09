package com.pasarela.compartido.infraestructura.bitacora;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bitacora_operaciones")
public class RegistroBitacoraJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private Instant momento;

	@Column(nullable = false, length = 50)
	private String tipo;

	@Column(nullable = false, length = 150)
	private String actor;

	@Column(nullable = false, length = 1000)
	private String detalle;

	protected RegistroBitacoraJpaEntity() {
		// requerido por JPA
	}

	RegistroBitacoraJpaEntity(UUID id, Instant momento, String tipo, String actor, String detalle) {
		this.id = id;
		this.momento = momento;
		this.tipo = tipo;
		this.actor = actor;
		this.detalle = detalle;
	}

}
