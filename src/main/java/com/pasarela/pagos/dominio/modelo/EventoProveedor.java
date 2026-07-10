package com.pasarela.pagos.dominio.modelo;

import com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro CRUDO de cada webhook recibido, tal cual llega y ANTES de
 * procesarlo (docs/04): es la red de seguridad para idempotencia, auditoría
 * y disputas. La clave de idempotencia es (proveedor, idExternoEvento) con
 * restricción única en BD.
 */
public class EventoProveedor {

	private final IdEventoProveedor id;
	private final String proveedor;
	private final String idExternoEvento;
	private final String tipo;
	private final String cargaCruda;
	private final boolean firmaValida;
	private final Instant recibidoEn;
	private boolean procesado;
	private String notaRevision;

	private EventoProveedor(IdEventoProveedor id, String proveedor, String idExternoEvento,
			String tipo, String cargaCruda, boolean firmaValida, Instant recibidoEn) {
		this.id = id;
		this.proveedor = proveedor;
		this.idExternoEvento = idExternoEvento;
		this.tipo = tipo;
		this.cargaCruda = cargaCruda;
		this.firmaValida = firmaValida;
		this.recibidoEn = recibidoEn;
	}

	/** Webhook con firma válida, listo para procesar. */
	public static EventoProveedor registrar(String proveedor, String idExternoEvento,
			String tipo, String cargaCruda, Instant recibidoEn) {
		validarTexto(idExternoEvento, "el id externo del evento");
		validarTexto(tipo, "el tipo del evento");
		return new EventoProveedor(IdEventoProveedor.generar(),
				validarProveedor(proveedor), idExternoEvento, tipo,
				validarCarga(cargaCruda), true, validarFecha(recibidoEn));
	}

	/** Intento con firma inválida: se registra para auditoría y nada más. */
	public static EventoProveedor registrarIntentoConFirmaInvalida(String proveedor,
			String cargaCruda, Instant recibidoEn) {
		return new EventoProveedor(IdEventoProveedor.generar(),
				validarProveedor(proveedor), null, null,
				validarCarga(cargaCruda), false, validarFecha(recibidoEn));
	}

	/** Rehidratación desde persistencia. */
	public static EventoProveedor reconstituir(IdEventoProveedor id, String proveedor,
			String idExternoEvento, String tipo, String cargaCruda, boolean firmaValida,
			boolean procesado, String notaRevision, Instant recibidoEn) {
		if (id == null) {
			throw new WebhookInvalidoException("El evento requiere id");
		}
		EventoProveedor evento = new EventoProveedor(id, validarProveedor(proveedor),
				idExternoEvento, tipo, validarCarga(cargaCruda), firmaValida,
				validarFecha(recibidoEn));
		evento.procesado = procesado;
		evento.notaRevision = notaRevision;
		return evento;
	}

	/** El pago quedó confirmado a partir de este evento. */
	public void marcarProcesado() {
		if (!firmaValida) {
			throw new WebhookInvalidoException(
					"Un evento con firma inválida jamás se procesa");
		}
		this.procesado = true;
	}

	/** El evento no pudo procesarse automáticamente: queda para el Admin. */
	public void marcarParaRevision(String nota) {
		validarTexto(nota, "la nota de revisión");
		this.notaRevision = nota;
	}

	private static String validarProveedor(String proveedor) {
		validarTexto(proveedor, "el proveedor");
		return proveedor;
	}

	private static String validarCarga(String cargaCruda) {
		validarTexto(cargaCruda, "la carga cruda");
		return cargaCruda;
	}

	private static Instant validarFecha(Instant recibidoEn) {
		if (recibidoEn == null) {
			throw new WebhookInvalidoException(
					"El evento requiere la fecha de recepción");
		}
		return recibidoEn;
	}

	private static void validarTexto(String valor, String nombre) {
		if (valor == null || valor.isBlank()) {
			throw new WebhookInvalidoException(
					"En un evento del proveedor, %s no puede estar vacío".formatted(nombre));
		}
	}

	public IdEventoProveedor id() {
		return id;
	}

	public String proveedor() {
		return proveedor;
	}

	/** Clave de idempotencia; null solo en intentos con firma inválida. */
	public String idExternoEvento() {
		return idExternoEvento;
	}

	public String tipo() {
		return tipo;
	}

	public String cargaCruda() {
		return cargaCruda;
	}

	public boolean firmaValida() {
		return firmaValida;
	}

	public boolean procesado() {
		return procesado;
	}

	public String notaRevision() {
		return notaRevision;
	}

	public Instant recibidoEn() {
		return recibidoEn;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof EventoProveedor otroEvento && id.equals(otroEvento.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
