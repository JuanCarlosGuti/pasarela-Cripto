package com.pasarela.comercios.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
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

	@Column(name = "cuenta_banco", nullable = false, length = 50)
	private String cuentaBanco;

	@Column(name = "cuenta_tipo", nullable = false, length = 20)
	private String cuentaTipo;

	@Column(name = "cuenta_numero", nullable = false, length = 30)
	private String cuentaNumero;

	@Column(name = "cuenta_titular", nullable = false, length = 200)
	private String cuentaTitular;

	@Column(name = "registrado_en", nullable = false)
	private Instant registradoEn;

	@Column(name = "motivo_decision", length = 500)
	private String motivoDecision;

	@Column(name = "decision_en")
	private Instant decisionEn;

	@Column(name = "limite_por_transaccion", nullable = false, precision = 19, scale = 4)
	private BigDecimal limitePorTransaccion;

	@Column(name = "limite_mensual", nullable = false, precision = 19, scale = 4)
	private BigDecimal limiteMensual;

	protected ComercioJpaEntity() {
		// requerido por JPA
	}

	ComercioJpaEntity(UUID id, String razonSocial, String nit, String estadoVerificacion,
			String cuentaBanco, String cuentaTipo, String cuentaNumero, String cuentaTitular,
			Instant registradoEn, String motivoDecision, Instant decisionEn,
			BigDecimal limitePorTransaccion, BigDecimal limiteMensual) {
		this.id = id;
		this.razonSocial = razonSocial;
		this.nit = nit;
		this.estadoVerificacion = estadoVerificacion;
		this.cuentaBanco = cuentaBanco;
		this.cuentaTipo = cuentaTipo;
		this.cuentaNumero = cuentaNumero;
		this.cuentaTitular = cuentaTitular;
		this.registradoEn = registradoEn;
		this.motivoDecision = motivoDecision;
		this.decisionEn = decisionEn;
		this.limitePorTransaccion = limitePorTransaccion;
		this.limiteMensual = limiteMensual;
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

	String cuentaBanco() {
		return cuentaBanco;
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

	String motivoDecision() {
		return motivoDecision;
	}

	Instant decisionEn() {
		return decisionEn;
	}

	BigDecimal limitePorTransaccion() {
		return limitePorTransaccion;
	}

	BigDecimal limiteMensual() {
		return limiteMensual;
	}

}