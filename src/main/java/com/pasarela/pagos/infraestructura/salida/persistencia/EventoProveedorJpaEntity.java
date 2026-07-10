package com.pasarela.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Representación JPA del evento crudo del proveedor. */
@Entity
@Table(name = "eventos_proveedor")
public class EventoProveedorJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 50)
	private String proveedor;

	@Column(name = "id_externo_evento", length = 100)
	private String idExternoEvento;

	@Column(length = 50)
	private String tipo;

	@Column(name = "carga_cruda", nullable = false)
	private String cargaCruda;

	@Column(name = "firma_valida", nullable = false)
	private boolean firmaValida;

	@Column(nullable = false)
	private boolean procesado;

	@Column(name = "nota_revision", length = 500)
	private String notaRevision;

	@Column(name = "recibido_en", nullable = false)
	private Instant recibidoEn;

	protected EventoProveedorJpaEntity() {
		// requerido por JPA
	}

	EventoProveedorJpaEntity(UUID id, String proveedor, String idExternoEvento, String tipo,
			String cargaCruda, boolean firmaValida, boolean procesado, String notaRevision,
			Instant recibidoEn) {
		this.id = id;
		this.proveedor = proveedor;
		this.idExternoEvento = idExternoEvento;
		this.tipo = tipo;
		this.cargaCruda = cargaCruda;
		this.firmaValida = firmaValida;
		this.procesado = procesado;
		this.notaRevision = notaRevision;
		this.recibidoEn = recibidoEn;
	}

	UUID id() {
		return id;
	}

	String proveedor() {
		return proveedor;
	}

	String idExternoEvento() {
		return idExternoEvento;
	}

	String tipo() {
		return tipo;
	}

	String cargaCruda() {
		return cargaCruda;
	}

	boolean firmaValida() {
		return firmaValida;
	}

	boolean procesado() {
		return procesado;
	}

	String notaRevision() {
		return notaRevision;
	}

	Instant recibidoEn() {
		return recibidoEn;
	}

}
