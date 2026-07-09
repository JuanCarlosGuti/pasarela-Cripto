package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.VerificacionInvalidaException;
import com.pasarela.compartido.dominio.modelo.IdComercio;

import java.time.Instant;
import java.util.Objects;

/**
 * Agregado raíz del contexto de comercios: el negocio que cobra por la
 * plataforma. Nace PENDIENTE y solo cobra cuando un Admin lo verifica.
 *
 * <p>Decisiones de verificación (HU-005) — cada acción exige su estado de
 * origen; todo lo demás se rechaza:</p>
 * <pre>
 * PENDIENTE  --verificar-->  VERIFICADO --suspender--> SUSPENDIDO
 * PENDIENTE  --rechazar-->   RECHAZADO (terminal: re-registro si se corrige)
 * SUSPENDIDO --reactivar-->  VERIFICADO (verificar un SUSPENDIDO se rechaza)
 * </pre>
 */
public class Comercio {

	private final IdComercio id;
	private final String razonSocial;
	private final Nit nit;
	private final CuentaLiquidacion cuentaLiquidacion;
	private final Instant registradoEn;
	private EstadoVerificacion estadoVerificacion;
	private String motivoDecision;
	private Instant decisionEn;

	private Comercio(IdComercio id, String razonSocial, Nit nit,
			CuentaLiquidacion cuentaLiquidacion, Instant registradoEn,
			EstadoVerificacion estadoVerificacion) {
		this.id = id;
		this.razonSocial = razonSocial;
		this.nit = nit;
		this.cuentaLiquidacion = cuentaLiquidacion;
		this.registradoEn = registradoEn;
		this.estadoVerificacion = estadoVerificacion;
	}

	public static Comercio registrar(String razonSocial, Nit nit,
			CuentaLiquidacion cuentaLiquidacion, Instant ahora) {
		validarRazonSocial(razonSocial);
		validarObligatorio(nit, "el NIT");
		validarObligatorio(cuentaLiquidacion, "la cuenta de liquidación");
		validarObligatorio(ahora, "la fecha de registro");
		return new Comercio(IdComercio.generar(), razonSocial.trim(), nit,
				cuentaLiquidacion, ahora, EstadoVerificacion.PENDIENTE);
	}

	/** Rehidratación desde persistencia: estado y última decisión tal cual. */
	public static Comercio reconstituir(IdComercio id, String razonSocial, Nit nit,
			CuentaLiquidacion cuentaLiquidacion, EstadoVerificacion estadoVerificacion,
			Instant registradoEn, String motivoDecision, Instant decisionEn) {
		validarObligatorio(id, "el id");
		validarRazonSocial(razonSocial);
		validarObligatorio(nit, "el NIT");
		validarObligatorio(cuentaLiquidacion, "la cuenta de liquidación");
		validarObligatorio(estadoVerificacion, "el estado de verificación");
		validarObligatorio(registradoEn, "la fecha de registro");
		Comercio comercio = new Comercio(id, razonSocial, nit, cuentaLiquidacion,
				registradoEn, estadoVerificacion);
		comercio.motivoDecision = motivoDecision;
		comercio.decisionEn = decisionEn;
		return comercio;
	}

	/** Aprobación del Admin: PENDIENTE → VERIFICADO. */
	public void verificar(Instant ahora) {
		exigirEstado(EstadoVerificacion.PENDIENTE, "verificar");
		aplicarDecision(EstadoVerificacion.VERIFICADO, null, ahora);
	}

	/** Rechazo del Admin (motivo obligatorio): PENDIENTE → RECHAZADO. */
	public void rechazar(String motivo, Instant ahora) {
		exigirEstado(EstadoVerificacion.PENDIENTE, "rechazar");
		aplicarDecision(EstadoVerificacion.RECHAZADO, exigirMotivo(motivo), ahora);
	}

	/** Suspensión del Admin (motivo obligatorio): VERIFICADO → SUSPENDIDO. */
	public void suspender(String motivo, Instant ahora) {
		exigirEstado(EstadoVerificacion.VERIFICADO, "suspender");
		aplicarDecision(EstadoVerificacion.SUSPENDIDO, exigirMotivo(motivo), ahora);
	}

	/** Reactivación explícita: SUSPENDIDO → VERIFICADO. */
	public void reactivar(Instant ahora) {
		exigirEstado(EstadoVerificacion.SUSPENDIDO, "reactivar");
		aplicarDecision(EstadoVerificacion.VERIFICADO, null, ahora);
	}

	/** Regla del MVP: solo un comercio verificado puede crear cobros. */
	public boolean puedeCobrar() {
		return estadoVerificacion == EstadoVerificacion.VERIFICADO;
	}

	private void exigirEstado(EstadoVerificacion esperado, String accion) {
		if (estadoVerificacion != esperado) {
			throw new VerificacionInvalidaException(
					"No se puede %s un comercio en estado %s (se requiere %s)"
							.formatted(accion, estadoVerificacion, esperado));
		}
	}

	private static String exigirMotivo(String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new ComercioInvalidoException(
					"Esta decisión de verificación requiere un motivo (auditoría)");
		}
		return motivo.trim();
	}

	private void aplicarDecision(EstadoVerificacion nuevoEstado, String motivo, Instant ahora) {
		validarObligatorio(ahora, "el momento de la decisión");
		this.estadoVerificacion = nuevoEstado;
		this.motivoDecision = motivo;
		this.decisionEn = ahora;
	}

	private static void validarRazonSocial(String razonSocial) {
		if (razonSocial == null || razonSocial.isBlank()) {
			throw new ComercioInvalidoException("La razón social es obligatoria");
		}
	}

	private static void validarObligatorio(Object valor, String nombre) {
		if (valor == null) {
			throw new ComercioInvalidoException(
					"En un comercio, %s no puede ser nulo".formatted(nombre));
		}
	}

	public IdComercio id() {
		return id;
	}

	public String razonSocial() {
		return razonSocial;
	}

	public Nit nit() {
		return nit;
	}

	public CuentaLiquidacion cuentaLiquidacion() {
		return cuentaLiquidacion;
	}

	public EstadoVerificacion estadoVerificacion() {
		return estadoVerificacion;
	}

	public Instant registradoEn() {
		return registradoEn;
	}

	/** Motivo de la última decisión (rechazo/suspensión); null si no aplica. */
	public String motivoDecision() {
		return motivoDecision;
	}

	/** Momento de la última decisión de verificación; null si no ha habido. */
	public Instant decisionEn() {
		return decisionEn;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof Comercio otroComercio && id.equals(otroComercio.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}