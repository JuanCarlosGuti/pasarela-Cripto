package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.compartido.dominio.modelo.IdComercio;

import java.time.Instant;
import java.util.Objects;

/**
 * Agregado raíz del contexto de comercios: el negocio que cobra por la
 * plataforma. Nace PENDIENTE de verificación y solo puede cobrar cuando un
 * Admin lo verifica (HU-005 define esas transiciones).
 */
public class Comercio {

	private final IdComercio id;
	private final String razonSocial;
	private final Nit nit;
	private final CuentaLiquidacion cuentaLiquidacion;
	private final Instant registradoEn;
	private EstadoVerificacion estadoVerificacion;

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
		if (razonSocial == null || razonSocial.isBlank()) {
			throw new ComercioInvalidoException("La razón social es obligatoria");
		}
		validarObligatorio(nit, "el NIT");
		validarObligatorio(cuentaLiquidacion, "la cuenta de liquidación");
		validarObligatorio(ahora, "la fecha de registro");
		return new Comercio(IdComercio.generar(), razonSocial.trim(), nit,
				cuentaLiquidacion, ahora, EstadoVerificacion.PENDIENTE);
	}

	/** Rehidratación desde persistencia: el estado se restaura tal cual. */
	public static Comercio reconstituir(IdComercio id, String razonSocial, Nit nit,
			CuentaLiquidacion cuentaLiquidacion, EstadoVerificacion estadoVerificacion,
			Instant registradoEn) {
		validarObligatorio(id, "el id");
		if (razonSocial == null || razonSocial.isBlank()) {
			throw new ComercioInvalidoException("La razón social es obligatoria");
		}
		validarObligatorio(nit, "el NIT");
		validarObligatorio(cuentaLiquidacion, "la cuenta de liquidación");
		validarObligatorio(estadoVerificacion, "el estado de verificación");
		validarObligatorio(registradoEn, "la fecha de registro");
		return new Comercio(id, razonSocial, nit, cuentaLiquidacion,
				registradoEn, estadoVerificacion);
	}

	/** Regla del MVP: solo un comercio verificado puede crear cobros. */
	public boolean puedeCobrar() {
		return estadoVerificacion == EstadoVerificacion.VERIFICADO;
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

	@Override
	public boolean equals(Object otro) {
		return otro instanceof Comercio otroComercio && id.equals(otroComercio.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}