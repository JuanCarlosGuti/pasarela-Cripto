package com.pasarela.liquidaciones.infraestructura.salida.persistencia;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Representación JPA de la liquidación, separada del dominio. */
@Entity
@Table(name = "liquidaciones")
public class LiquidacionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "comercio_id", nullable = false)
	private UUID comercioId;

	@Column(name = "monto_bruto", nullable = false, precision = 19, scale = 4)
	private BigDecimal montoBruto;

	@Column(name = "comision_plataforma", nullable = false, precision = 19, scale = 4)
	private BigDecimal comisionPlataforma;

	@Column(name = "monto_neto_comercio", nullable = false, precision = 19, scale = 4)
	private BigDecimal montoNetoComercio;

	@Column(name = "referencia_proveedor", nullable = false, length = 100)
	private String referenciaProveedor;

	@Column(nullable = false, length = 20)
	private String estado;

	@Column(name = "liquidada_en", nullable = false)
	private Instant liquidadaEn;

	@Column(name = "detalle_discrepancia", length = 1000)
	private String detalleDiscrepancia;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "liquidacion_ordenes",
			joinColumns = @JoinColumn(name = "liquidacion_id"))
	@Column(name = "orden_id", nullable = false)
	private List<UUID> ordenes = new ArrayList<>();

	protected LiquidacionJpaEntity() {
		// requerido por JPA
	}

	LiquidacionJpaEntity(UUID id, UUID comercioId, BigDecimal montoBruto,
			BigDecimal comisionPlataforma, BigDecimal montoNetoComercio,
			String referenciaProveedor, String estado, Instant liquidadaEn,
			List<UUID> ordenes, String detalleDiscrepancia) {
		this.id = id;
		this.comercioId = comercioId;
		this.montoBruto = montoBruto;
		this.comisionPlataforma = comisionPlataforma;
		this.montoNetoComercio = montoNetoComercio;
		this.referenciaProveedor = referenciaProveedor;
		this.estado = estado;
		this.liquidadaEn = liquidadaEn;
		this.ordenes = ordenes;
		this.detalleDiscrepancia = detalleDiscrepancia;
	}

	UUID id() {
		return id;
	}

	UUID comercioId() {
		return comercioId;
	}

	BigDecimal montoBruto() {
		return montoBruto;
	}

	BigDecimal comisionPlataforma() {
		return comisionPlataforma;
	}

	BigDecimal montoNetoComercio() {
		return montoNetoComercio;
	}

	String referenciaProveedor() {
		return referenciaProveedor;
	}

	String estado() {
		return estado;
	}

	Instant liquidadaEn() {
		return liquidadaEn;
	}

	List<UUID> ordenes() {
		return ordenes;
	}

	String detalleDiscrepancia() {
		return detalleDiscrepancia;
	}

}
