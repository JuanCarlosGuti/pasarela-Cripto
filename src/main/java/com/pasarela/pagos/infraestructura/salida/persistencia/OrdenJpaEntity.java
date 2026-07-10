package com.pasarela.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representación JPA de la orden, separada del dominio a propósito: el
 * modelo de negocio no conoce la persistencia (ArchUnit lo garantiza).
 * El mapper traduce en ambas direcciones.
 */
@Entity
@Table(name = "ordenes")
public class OrdenJpaEntity {

	@Id
	private UUID id;

	@Column(name = "comercio_id", nullable = false)
	private UUID comercioId;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal monto;

	@Column(nullable = false, length = 10)
	private String moneda;

	@Column(nullable = false, length = 64)
	private String referencia;

	@Column(nullable = false, length = 20)
	private String estado;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "orden_transiciones", joinColumns = @JoinColumn(name = "orden_id"))
	@OrderColumn(name = "indice")
	private List<TransicionEstadoEmbeddable> transiciones = new ArrayList<>();

	/** Bloqueo optimista (HU-014): resuelve la carrera expiración-vs-pago. */
	@Version
	@Column(nullable = false)
	private Long version;

	protected OrdenJpaEntity() {
		// requerido por JPA
	}

	OrdenJpaEntity(UUID id, UUID comercioId, BigDecimal monto, String moneda,
			String referencia, String estado, Instant creadaEn, Instant expiraEn,
			List<TransicionEstadoEmbeddable> transiciones, Long version) {
		this.id = id;
		this.comercioId = comercioId;
		this.monto = monto;
		this.moneda = moneda;
		this.referencia = referencia;
		this.estado = estado;
		this.creadaEn = creadaEn;
		this.expiraEn = expiraEn;
		this.transiciones = transiciones;
		this.version = version;
	}

	UUID id() {
		return id;
	}

	UUID comercioId() {
		return comercioId;
	}

	BigDecimal monto() {
		return monto;
	}

	String moneda() {
		return moneda;
	}

	String referencia() {
		return referencia;
	}

	String estado() {
		return estado;
	}

	Instant creadaEn() {
		return creadaEn;
	}

	Instant expiraEn() {
		return expiraEn;
	}

	List<TransicionEstadoEmbeddable> transiciones() {
		return transiciones;
	}

	Long version() {
		return version;
	}

}