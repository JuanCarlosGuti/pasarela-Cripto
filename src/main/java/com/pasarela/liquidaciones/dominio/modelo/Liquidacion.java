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
 * <p>Invariantes del dinero (HU-016, ampliado en HU-025): {@code bruto =
 * Σ órdenes} y {@code neto = bruto − comisiónPlataforma − comisiónRampa}, al
 * centavo. El neto se define POR DIFERENCIA: comisiónPlataforma +
 * comisiónRampa + neto suman exactamente el bruto, siempre. La rehidratación
 * re-verifica la suma: montos que no cuadran jamás entran.</p>
 */
public class Liquidacion {

	private final IdLiquidacion id;
	private final IdComercio comercioId;
	private final List<IdOrden> ordenes;
	private final Dinero montoBruto;
	private final Dinero comisionPlataforma;
	private final Dinero montoNetoComercio;
	private final String referenciaProveedor;
	private final DetalleRampa detalleRampa;
	private final Instant liquidadaEn;
	private EstadoLiquidacion estado;
	private String detalleDiscrepancia;

	private Liquidacion(IdLiquidacion id, IdComercio comercioId, List<IdOrden> ordenes,
			Dinero montoBruto, Dinero comisionPlataforma, Dinero montoNetoComercio,
			String referenciaProveedor, DetalleRampa detalleRampa, Instant liquidadaEn,
			EstadoLiquidacion estado) {
		this.id = id;
		this.comercioId = comercioId;
		this.ordenes = List.copyOf(ordenes);
		this.montoBruto = montoBruto;
		this.comisionPlataforma = comisionPlataforma;
		this.montoNetoComercio = montoNetoComercio;
		this.referenciaProveedor = referenciaProveedor;
		this.detalleRampa = detalleRampa;
		this.liquidadaEn = liquidadaEn;
		this.estado = estado;
	}

	public static Liquidacion registrar(IdComercio comercioId, List<OrdenLiquidable> ordenes,
			Porcentaje tasaComision, String referenciaProveedor, DetalleRampa detalleRampa,
			Instant ahora) {
		validarObligatorio(comercioId, "el comercio");
		validarObligatorio(tasaComision, "la tasa de comisión");
		validarObligatorio(detalleRampa, "el detalle de la rampa");
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
		Dinero neto = bruto.restar(comision).restar(detalleRampa.comisionRampa());
		return new Liquidacion(IdLiquidacion.generar(), comercioId,
				ordenes.stream().map(OrdenLiquidable::id).toList(),
				bruto, comision, neto, referenciaProveedor.trim(), detalleRampa, ahora,
				EstadoLiquidacion.REGISTRADA);
	}

	/**
	 * Concilia lo registrado contra lo reportado por el proveedor (HU-017):
	 * si todo coincide pasa a CONCILIADA; CUALQUIER diferencia (monto,
	 * órdenes faltantes o sobrantes) la deja en DISCREPANCIA con el detalle
	 * completo — jamás se cuadra en silencio. Solo una liquidación
	 * REGISTRADA puede conciliarse.
	 */
	public void conciliar(ReporteDelProveedor reporte) {
		if (reporte == null) {
			throw new LiquidacionInvalidaException(
					"La conciliación requiere el reporte del proveedor");
		}
		if (estado != EstadoLiquidacion.REGISTRADA) {
			throw new com.pasarela.liquidaciones.dominio.excepcion.ConciliacionInvalidaException(
					"La liquidación ya fue conciliada (estado actual: " + estado + ")");
		}
		StringBuilder diferencias = new StringBuilder();
		if (!montoBruto.equals(reporte.montoBruto())) {
			diferencias.append("monto bruto registrado %s vs reportado %s; ".formatted(
					montoBruto.monto().toPlainString(),
					reporte.montoBruto().monto().toPlainString()));
		}
		List<IdOrden> faltantes = ordenes.stream()
				.filter(orden -> !reporte.ordenes().contains(orden)).toList();
		List<IdOrden> sobrantes = reporte.ordenes().stream()
				.filter(orden -> !ordenes.contains(orden)).toList();
		if (!faltantes.isEmpty()) {
			diferencias.append("órdenes registradas que el proveedor no reporta: %s; ".formatted(
					faltantes.stream().map(orden -> orden.valor().toString()).toList()));
		}
		if (!sobrantes.isEmpty()) {
			diferencias.append("órdenes reportadas que no están registradas: %s; ".formatted(
					sobrantes.stream().map(orden -> orden.valor().toString()).toList()));
		}
		if (diferencias.isEmpty()) {
			this.estado = EstadoLiquidacion.CONCILIADA;
			this.detalleDiscrepancia = null;
		} else {
			this.estado = EstadoLiquidacion.DISCREPANCIA;
			this.detalleDiscrepancia = diferencias.toString().trim();
		}
	}

	/** Rehidratación desde persistencia; re-verifica que el dinero cuadre. */
	public static Liquidacion reconstituir(IdLiquidacion id, IdComercio comercioId,
			List<IdOrden> ordenes, Dinero montoBruto, Dinero comisionPlataforma,
			Dinero montoNetoComercio, String referenciaProveedor, DetalleRampa detalleRampa,
			EstadoLiquidacion estado, Instant liquidadaEn, String detalleDiscrepancia) {
		validarObligatorio(id, "el id");
		validarObligatorio(comercioId, "el comercio");
		validarObligatorio(detalleRampa, "el detalle de la rampa");
		validarObligatorio(estado, "el estado");
		validarObligatorio(liquidadaEn, "la fecha de liquidación");
		if (ordenes == null || ordenes.isEmpty()) {
			throw new LiquidacionInvalidaException(
					"La liquidación requiere al menos una orden");
		}
		if (!comisionPlataforma.sumar(detalleRampa.comisionRampa()).sumar(montoNetoComercio)
				.equals(montoBruto)) {
			throw new LiquidacionInvalidaException(
					"Los montos de la liquidación no cuadran: %s + %s + %s ≠ %s".formatted(
							comisionPlataforma.monto().toPlainString(),
							detalleRampa.comisionRampa().monto().toPlainString(),
							montoNetoComercio.monto().toPlainString(),
							montoBruto.monto().toPlainString()));
		}
		Liquidacion liquidacion = new Liquidacion(id, comercioId, ordenes, montoBruto,
				comisionPlataforma, montoNetoComercio, referenciaProveedor, detalleRampa,
				liquidadaEn, estado);
		liquidacion.detalleDiscrepancia = detalleDiscrepancia;
		return liquidacion;
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

	public DetalleRampa detalleRampa() {
		return detalleRampa;
	}

	public EstadoLiquidacion estado() {
		return estado;
	}

	/** Detalle de la discrepancia detectada por la conciliación; null si no hay. */
	public String detalleDiscrepancia() {
		return detalleDiscrepancia;
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
