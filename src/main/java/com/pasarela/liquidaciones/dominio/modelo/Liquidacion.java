package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.compartido.dominio.puerto.LiquidadorDeOrdenes.OrdenLiquidable;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Agregado raíz del contexto de liquidaciones (docs/04): registra que el
 * proveedor liquidó COP al comercio por un grupo de órdenes. La plataforma
 * NO mueve el dinero; solo lo registra y concilia (REGLA DE ORO).
 *
 * <p>Invariantes del dinero (HU-016): {@code bruto = Σ órdenes} y
 * {@code neto = bruto − comisión}, al centavo. El neto se define POR
 * DIFERENCIA: comisión + neto suman exactamente el bruto, siempre. La
 * rehidratación re-verifica la suma: montos que no cuadran jamás entran.</p>
 */
public class Liquidacion {

	private final IdLiquidacion id;
	private final IdComercio comercioId;
	private final List<IdOrden> ordenes;
	private final Dinero montoBruto;
	private final Dinero comisionPlataforma;
	private final Dinero montoNetoComercio;
	private final String referenciaProveedor;
	private final Instant liquidadaEn;
	private EstadoLiquidacion estado;

	private Liquidacion(IdLiquidacion id, IdComercio comercioId, List<IdOrden> ordenes,
			Dinero montoBruto, Dinero comisionPlataforma, Dinero montoNetoComercio,
			String referenciaProveedor, Instant liquidadaEn, EstadoLiquidacion estado) {
		this.id = id;
		this.comercioId = comercioId;
		this.ordenes = List.copyOf(ordenes);
		this.montoBruto = montoBruto;
		this.comisionPlataforma = comisionPlataforma;
		this.montoNetoComercio = montoNetoComercio;
		this.referenciaProveedor = referenciaProveedor;
		this.liquidadaEn = liquidadaEn;
		this.estado = estado;
	}

	public static Liquidacion registrar(IdComercio comercioId, List<OrdenLiquidable> ordenes,
			Porcentaje tasaComision, String referenciaProveedor, Instant ahora) {
		validarObligatorio(comercioId, "el comercio");
		validarObligatorio(tasaComision, "la tasa de comisión");
		validarObligatorio(ahora, "la fecha de liquidación");
		if (referenciaProveedor == null || referenciaProveedor.isBlank()) {
			throw new LiquidacionInvalidaException(
					"La liquidación requiere la referencia del proveedor");
		}
		if (ordenes == null || ordenes.isEmpty()) {
			throw new LiquidacionInvalidaException(
					"La liquidación requiere al menos una orden");
		}
		Set<IdOrden> sinRepetidas = new HashSet<>();
		for (OrdenLiquidable orden : ordenes) {
			if (!sinRepetidas.add(orden.id())) {
				throw new LiquidacionInvalidaException(
						"La orden %s está repetida en la liquidación".formatted(
								orden.id().valor()));
			}
		}
		Dinero bruto = ordenes.stream().map(OrdenLiquidable::monto)
				.reduce(Dinero.cop(0), Dinero::sumar);
		Dinero comision = bruto.porcentaje(tasaComision);
		Dinero neto = bruto.restar(comision);
		return new Liquidacion(IdLiquidacion.generar(), comercioId,
				ordenes.stream().map(OrdenLiquidable::id).toList(),
				bruto, comision, neto, referenciaProveedor.trim(), ahora,
				EstadoLiquidacion.REGISTRADA);
	}

	/** Rehidratación desde persistencia; re-verifica que el dinero cuadre. */
	public static Liquidacion reconstituir(IdLiquidacion id, IdComercio comercioId,
			List<IdOrden> ordenes, Dinero montoBruto, Dinero comisionPlataforma,
			Dinero montoNetoComercio, String referenciaProveedor,
			EstadoLiquidacion estado, Instant liquidadaEn) {
		validarObligatorio(id, "el id");
		validarObligatorio(comercioId, "el comercio");
		validarObligatorio(estado, "el estado");
		validarObligatorio(liquidadaEn, "la fecha de liquidación");
		if (ordenes == null || ordenes.isEmpty()) {
			throw new LiquidacionInvalidaException(
					"La liquidación requiere al menos una orden");
		}
		if (!comisionPlataforma.sumar(montoNetoComercio).equals(montoBruto)) {
			throw new LiquidacionInvalidaException(
					"Los montos de la liquidación no cuadran: %s + %s ≠ %s".formatted(
							comisionPlataforma.monto().toPlainString(),
							montoNetoComercio.monto().toPlainString(),
							montoBruto.monto().toPlainString()));
		}
		return new Liquidacion(id, comercioId, ordenes, montoBruto, comisionPlataforma,
				montoNetoComercio, referenciaProveedor, liquidadaEn, estado);
	}

	private static void validarObligatorio(Object valor, String nombre) {
		if (valor == null) {
			throw new LiquidacionInvalidaException(
					"En una liquidación, %s no puede ser nulo".formatted(nombre));
		}
	}

	public IdLiquidacion id() {
		return id;
	}

	public IdComercio comercioId() {
		return comercioId;
	}

	public List<IdOrden> ordenes() {
		return ordenes;
	}

	public Dinero montoBruto() {
		return montoBruto;
	}

	public Dinero comisionPlataforma() {
		return comisionPlataforma;
	}

	public Dinero montoNetoComercio() {
		return montoNetoComercio;
	}

	public String referenciaProveedor() {
		return referenciaProveedor;
	}

	public EstadoLiquidacion estado() {
		return estado;
	}

	public Instant liquidadaEn() {
		return liquidadaEn;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof Liquidacion otraLiquidacion && id.equals(otraLiquidacion.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
