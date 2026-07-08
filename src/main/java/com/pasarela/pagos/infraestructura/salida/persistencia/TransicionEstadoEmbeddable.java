package com.pasarela.pagos.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/** Fila del historial de transiciones (tabla orden_transiciones). */
@Embeddable
public class TransicionEstadoEmbeddable {

	@Column(name = "desde", nullable = false, length = 20)
	private String desde;

	@Column(name = "hacia", nullable = false, length = 20)
	private String hacia;

	@Column(name = "momento", nullable = false)
	private Instant momento;

	@Column(name = "motivo", length = 500)
	private String motivo;

	protected TransicionEstadoEmbeddable() {
		// requerido por JPA
	}

	TransicionEstadoEmbeddable(String desde, String hacia, Instant momento, String motivo) {
		this.desde = desde;
		this.hacia = hacia;
		this.momento = momento;
		this.motivo = motivo;
	}

	String desde() {
		return desde;
	}

	String hacia() {
		return hacia;
	}

	Instant momento() {
		return momento;
	}

	String motivo() {
		return motivo;
	}

}