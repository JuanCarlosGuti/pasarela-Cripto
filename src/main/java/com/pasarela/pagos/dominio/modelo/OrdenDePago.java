package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.excepcion.OrdenNoPuedeConfirmarseException;
import com.pasarela.pagos.dominio.excepcion.TransicionDeEstadoInvalidaException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agregado raíz del contexto de pagos: un cobro que un comercio le hace a un
 * pagador.
 *
 * <p>Invariantes:</p>
 * <ul>
 *   <li>El estado solo cambia por los métodos de negocio, que validan la
 *       transición contra la matriz de {@link EstadoOrden}. No hay
 *       {@code setEstado}.</li>
 *   <li>Toda transición queda registrada en el historial con su timestamp
 *       (auditoría y conciliación).</li>
 *   <li>El reloj se inyecta ({@code Instant ahora} como parámetro): ninguna
 *       regla usa {@code Instant.now()} — requisito de testeabilidad.</li>
 * </ul>
 */
public class OrdenDePago {

	private final IdOrden id;
	private final IdComercio comercioId;
	private final Dinero monto;
	private final ReferenciaPago referencia;
	private final Instant creadaEn;
	private final Instant expiraEn;
	private EstadoOrden estado;
	private final List<TransicionEstado> historial = new ArrayList<>();
	/**
	 * Marca opaca de concurrencia optimista: el dominio no la interpreta ni
	 * la modifica; la persistencia la usa para detectar la carrera
	 * expiración-vs-pago (HU-014). Null en órdenes aún no persistidas.
	 */
	private Long version;

	private OrdenDePago(IdOrden id, IdComercio comercioId, Dinero monto,
			ReferenciaPago referencia, Instant creadaEn, Instant expiraEn) {
		this.id = id;
		this.comercioId = comercioId;
		this.monto = monto;
		this.referencia = referencia;
		this.creadaEn = creadaEn;
		this.expiraEn = expiraEn;
		this.estado = EstadoOrden.CREADA;
	}

	public static OrdenDePago crear(IdComercio comercioId, Dinero monto,
			ReferenciaPago referencia, Instant creadaEn, Instant expiraEn) {
		validarObligatorio(comercioId, "el comercio");
		validarObligatorio(monto, "el monto");
		validarObligatorio(referencia, "la referencia de pago");
		validarObligatorio(creadaEn, "la fecha de creación");
		validarObligatorio(expiraEn, "la fecha de expiración");
		if (monto.esCero()) {
			throw new OrdenInvalidaException("El monto de la orden debe ser mayor que cero");
		}
		if (!expiraEn.isAfter(creadaEn)) {
			throw new OrdenInvalidaException(
					"La expiración debe ser posterior a la creación de la orden");
		}
		return new OrdenDePago(IdOrden.generar(), comercioId, monto, referencia, creadaEn, expiraEn);
	}

	/**
	 * Rehidrata una orden desde la persistencia: estado e historial se
	 * restauran tal cual, sin re-validar transiciones (ya fueron validadas
	 * cuando ocurrieron). Solo para adaptadores de persistencia — el resto
	 * del código crea órdenes con {@link #crear}.
	 */
	public static OrdenDePago reconstituir(IdOrden id, IdComercio comercioId, Dinero monto,
			ReferenciaPago referencia, Instant creadaEn, Instant expiraEn,
			EstadoOrden estado, List<TransicionEstado> historial, Long version) {
		validarObligatorio(id, "el id");
		validarObligatorio(comercioId, "el comercio");
		validarObligatorio(monto, "el monto");
		validarObligatorio(referencia, "la referencia de pago");
		validarObligatorio(creadaEn, "la fecha de creación");
		validarObligatorio(expiraEn, "la fecha de expiración");
		validarObligatorio(estado, "el estado");
		validarObligatorio(historial, "el historial");
		OrdenDePago orden = new OrdenDePago(id, comercioId, monto, referencia, creadaEn, expiraEn);
		orden.estado = estado;
		orden.historial.addAll(historial);
		orden.version = version;
		return orden;
	}

	/** El cobro quedó registrado en el proveedor (QR generado): CREADA → PENDIENTE_PAGO. */
	public void registrarCobroEnProveedor(Instant ahora) {
		transicionar(EstadoOrden.PENDIENTE_PAGO, ahora, null);
	}

	/** El proveedor reportó el pago: PENDIENTE_PAGO no expirada → PAGO_DETECTADO. */
	public void confirmarPago(EventoPago evento, Instant ahora) {
		if (evento == null) {
			throw new OrdenInvalidaException("El evento de pago no puede ser nulo");
		}
		if (!puedeConfirmarse()) {
			throw new OrdenNoPuedeConfirmarseException(
					"No se puede confirmar el pago de una orden en estado " + estado);
		}
		if (estaExpirada(ahora)) {
			throw new OrdenNoPuedeConfirmarseException(
					"No se puede confirmar el pago: la orden expiró en " + expiraEn);
		}
		transicionar(EstadoOrden.PAGO_DETECTADO, ahora, null);
	}

	/** Venció la ventana de pago sin que llegara el pago: PENDIENTE_PAGO → EXPIRADA. */
	public void expirar(Instant ahora) {
		if (!estaExpirada(ahora)) {
			throw new TransicionDeEstadoInvalidaException(
					"No se puede expirar la orden antes de su vencimiento (" + expiraEn + ")");
		}
		transicionar(EstadoOrden.EXPIRADA, ahora, null);
	}

	/** El proveedor convirtió la cripto a COP: PAGO_DETECTADO → CONVERTIDA. */
	public void marcarComoConvertida(Instant ahora) {
		transicionar(EstadoOrden.CONVERTIDA, ahora, null);
	}

	/** El proveedor liquidó los COP al comercio: CONVERTIDA → LIQUIDADA. */
	public void marcarComoLiquidada(Instant ahora) {
		transicionar(EstadoOrden.LIQUIDADA, ahora, null);
	}

	/** Error o pago inválido: PAGO_DETECTADO → FALLIDA. El motivo es obligatorio (auditoría). */
	public void marcarComoFallida(String motivo, Instant ahora) {
		if (motivo == null || motivo.isBlank()) {
			throw new OrdenInvalidaException(
					"Marcar la orden como fallida requiere un motivo");
		}
		transicionar(EstadoOrden.FALLIDA, ahora, motivo);
	}

	/** El fallo requiere intervención manual: FALLIDA → EN_REVISION. */
	public void escalarARevision(Instant ahora) {
		transicionar(EstadoOrden.EN_REVISION, ahora, null);
	}

	/** ¿Venció la ventana de pago? El límite exacto todavía NO está expirado. */
	public boolean estaExpirada(Instant ahora) {
		return ahora.isAfter(expiraEn);
	}

	public boolean puedeConfirmarse() {
		return estado == EstadoOrden.PENDIENTE_PAGO;
	}

	private void transicionar(EstadoOrden nuevoEstado, Instant momento, String motivo) {
		validarTransicionA(nuevoEstado);
		// el momento nulo lo rechaza el propio constructor de TransicionEstado,
		// antes de tocar historial o estado
		historial.add(new TransicionEstado(estado, nuevoEstado, momento, motivo));
		estado = nuevoEstado;
	}

	private void validarTransicionA(EstadoOrden nuevoEstado) {
		if (!estado.puedeTransicionarA(nuevoEstado)) {
			throw new TransicionDeEstadoInvalidaException(
					"Transición inválida de %s a %s".formatted(estado, nuevoEstado));
		}
	}

	private static void validarObligatorio(Object valor, String nombre) {
		if (valor == null) {
			throw new OrdenInvalidaException(
					"En una orden de pago, %s no puede ser nulo".formatted(nombre));
		}
	}

	public IdOrden id() {
		return id;
	}

	public IdComercio comercioId() {
		return comercioId;
	}

	public Dinero monto() {
		return monto;
	}

	public ReferenciaPago referencia() {
		return referencia;
	}

	public EstadoOrden estado() {
		return estado;
	}

	public Instant creadaEn() {
		return creadaEn;
	}

	public Instant expiraEn() {
		return expiraEn;
	}

	/** Copia inmutable: el historial solo crece por transiciones de la propia orden. */
	public List<TransicionEstado> historial() {
		return List.copyOf(historial);
	}

	/** Marca opaca de concurrencia optimista; null si la orden no viene de BD. */
	public Long version() {
		return version;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof OrdenDePago otraOrden && id.equals(otraOrden.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}