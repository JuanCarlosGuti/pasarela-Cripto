package com.pasarela.comercios.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Representación JPA del comercio, separada del dominio. */
@Entity
@Table(name = "comercios")
public class ComercioJpaEntity {

	@Id
	private UUID id;

	@Column(name = "razon_social", nullable = false, length = 200)
	private String razonSocial;

	@Column(nullable = false, length = 20)
	private String nit;

	@Column(name = "estado_verificacion", nullable = false, length = 20)
	private String estadoVerificacion;

	@Column(name = "cuenta_tipo", nullable = false, length = 20)
	private String cuentaTipo;

	@Column(name = "cuenta_numero", nullable = false, length = 30)
	private String cuentaNumero;

	@Column(name = "cuenta_titular", nullable = false, length = 200)
	private String cuentaTitular;

	@Column(name = "registrado_en", nullable = false)
	private Instant registradoEn;

	protected ComercioJpaEntity() {
		// requerido por JPA
	}

	ComercioJpaEntity(UUID id, String razonSocial, String nit, String estadoVerificacion,
			String cuentaTipo, String cuentaNumero, String cuentaTitular, Instant registradoEn) {
		this.id = id;
		this.razonSocial = razonSocial;
		this.nit = nit;
		this.estadoVerificacion = estadoVerificacion;
		this.cuentaTipo = cuentaTipo;
		this.cuentaNumero = cuentaNumero;
		this.cuentaTitular = cuentaTitular;
		this.registradoEn = registradoEn;
	}

	UUID id() {
		return id;
	}

	String razonSocial() {
		return razonSocial;
	}

	String nit() {
		return nit;
	}

	String estadoVerificacion() {
		return estadoVerificacion;
	}

	String cuentaTipo() {
		return cuentaTipo;
	}

	String cuentaNumero() {
		return cuentaNumero;
	}

	String cuentaTitular() {
		return cuentaTitular;
	}

	Instant registradoEn() {
		return registradoEn;
	}

}