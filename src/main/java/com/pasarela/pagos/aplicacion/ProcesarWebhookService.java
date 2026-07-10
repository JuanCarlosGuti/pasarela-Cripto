package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones.RegistroDeOperacion;
import com.pasarela.pagos.dominio.excepcion.FirmaDeWebhookInvalidaException;
import com.pasarela.pagos.dominio.excepcion.OrdenNoPuedeConfirmarseException;
import com.pasarela.pagos.dominio.modelo.EventoPago;
import com.pasarela.pagos.dominio.modelo.EventoProveedor;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio.EventoDuplicadoException;
import com.pasarela.pagos.dominio.puerto.salida.NotificadorDeComercios;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.WebhookDelProveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Procesa el webhook de pago (HU-010) en el orden EXACTO de docs/05:
 * <ol>
 *   <li>validar la firma (inválida → intento registrado + 401),</li>
 *   <li>guardar el evento CRUDO antes de cualquier efecto,</li>
 *   <li>idempotencia: visto antes → DUPLICADO sin efectos; la constraint
 *       única en BD es la última línea de defensa ante carreras,</li>
 *   <li>confirmar la orden (inexistente → evento a revisión + bitácora),</li>
 *   <li>notificar al comercio (best-effort: su fallo no revierte nada).</li>
 * </ol>
 * La interpretación del payload ocurre justo tras la firma porque el id
 * externo (clave de idempotencia) viaja dentro del payload; los EFECTOS
 * siguen el orden documentado.
 *
 * <p>El evento crudo se persiste en transacción propia (el adaptador usa
 * REQUIRES_NEW): sobrevive aunque el resto del procesamiento falle.</p>
 */
@Service
public class ProcesarWebhookService implements ProcesarWebhookUseCase {

	private static final Logger log = LoggerFactory.getLogger(ProcesarWebhookService.class);

	private final EventoProveedorRepositorio eventos;
	private final OrdenDePagoRepositorio ordenes;
	private final ProveedorDePagoPort proveedor;
	private final NotificadorDeComercios notificador;
	private final BitacoraDeOperaciones bitacora;
	private final Clock reloj;

	public ProcesarWebhookService(EventoProveedorRepositorio eventos,
			OrdenDePagoRepositorio ordenes, ProveedorDePagoPort proveedor,
			NotificadorDeComercios notificador, BitacoraDeOperaciones bitacora, Clock reloj) {
		this.eventos = eventos;
		this.ordenes = ordenes;
		this.proveedor = proveedor;
		this.notificador = notificador;
		this.bitacora = bitacora;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public ResultadoWebhook procesar(ComandoProcesarWebhook comando) {
		Instant ahora = reloj.instant();

		if (!proveedor.firmaValida(comando.cargaCruda(), comando.firma())) {
			eventos.guardar(EventoProveedor.registrarIntentoConFirmaInvalida(
					comando.proveedor(), comando.cargaCruda(), ahora));
			throw new FirmaDeWebhookInvalidaException(
					"La firma del webhook no corresponde al proveedor " + comando.proveedor());
		}

		WebhookDelProveedor webhook = proveedor.interpretarWebhook(comando.cargaCruda());
		if (eventos.existe(comando.proveedor(), webhook.idExternoEvento())) {
			return ResultadoWebhook.DUPLICADO;
		}

		EventoProveedor evento;
		try {
			evento = eventos.guardar(EventoProveedor.registrar(
					comando.proveedor(), webhook.idExternoEvento(), webhook.tipo(),
					comando.cargaCruda(), ahora));
		} catch (EventoDuplicadoException carrera) {
			return ResultadoWebhook.DUPLICADO;
		}

		Optional<OrdenDePago> ordenEncontrada = ordenes.buscarPorReferencia(webhook.referencia());
		if (ordenEncontrada.isEmpty()) {
			return dejarParaRevision(evento, comando.proveedor(), ahora,
					"WEBHOOK_SIN_ORDEN",
					"No existe una orden con la referencia del webhook",
					"Evento %s sin orden correspondiente (referencia %s)".formatted(
							webhook.idExternoEvento(), webhook.referencia().valor()));
		}
		OrdenDePago orden = ordenEncontrada.get();

		// camino triste: webhook fuera de orden o tipo aún no soportado (HU-012)
		if (!"PAGO_RECIBIDO".equals(webhook.tipo())) {
			return dejarParaRevision(evento, comando.proveedor(), ahora,
					"WEBHOOK_FUERA_DE_ORDEN",
					"Tipo de evento no aplicable al estado de la orden: " + webhook.tipo(),
					"Evento %s de tipo %s para la orden %s (estado %s): la máquina de estados lo tolera sin corromper nada".formatted(
							webhook.idExternoEvento(), webhook.tipo(),
							orden.id().valor(), orden.estado()));
		}

		// camino triste: monto distinto al esperado → EN_REVISION (HU-012)
		if (!orden.monto().equals(webhook.monto())) {
			orden.marcarComoFallida(
					"Monto del pago distinto al esperado: se esperaba %s y llegó %s".formatted(
							orden.monto().monto().toPlainString(),
							webhook.monto().monto().toPlainString()),
					ahora);
			orden.escalarARevision(ahora);
			ordenes.guardar(orden);
			return dejarParaRevision(evento, comando.proveedor(), ahora,
					"MONTO_DISTINTO",
					"Monto del pago distinto al esperado",
					"Evento %s con monto %s para la orden %s que esperaba %s: orden en revisión".formatted(
							webhook.idExternoEvento(), webhook.monto().monto().toPlainString(),
							orden.id().valor(), orden.monto().monto().toPlainString()));
		}

		try {
			orden.confirmarPago(
					new EventoPago(webhook.referencia(), webhook.monto(), webhook.pagadoEn()),
					ahora);
		} catch (OrdenNoPuedeConfirmarseException tardio) {
			// camino triste: pago tardío o estado no confirmable → revisión manual
			// con todos los datos para gestionar el reembolso vía proveedor (HU-012)
			return dejarParaRevision(evento, comando.proveedor(), ahora,
					"PAGO_TARDIO",
					"El pago no puede confirmarse: " + tardio.getMessage(),
					"Evento %s para la orden %s (estado %s, expiraba %s): requiere gestión manual del reembolso".formatted(
							webhook.idExternoEvento(), orden.id().valor(),
							orden.estado(), orden.expiraEn()));
		}
		ordenes.guardar(orden);
		notificarSinRevertir(orden);
		evento.marcarProcesado();
		eventos.guardar(evento);
		return ResultadoWebhook.CONFIRMADO;
	}

	private ResultadoWebhook dejarParaRevision(EventoProveedor evento, String proveedorNombre,
			Instant ahora, String tipoBitacora, String notaRevision, String detalleBitacora) {
		evento.marcarParaRevision(notaRevision);
		eventos.guardar(evento);
		bitacora.registrar(new RegistroDeOperacion(
				tipoBitacora, proveedorNombre, detalleBitacora, ahora));
		return ResultadoWebhook.PARA_REVISION;
	}

	private void notificarSinRevertir(OrdenDePago orden) {
		try {
			notificador.pagoDetectado(orden);
		} catch (RuntimeException fallo) {
			// best-effort (HU-013): la fuente de verdad es el estado de la orden
			log.warn("La notificación al comercio falló para la orden {}: {}",
					orden.id().valor(), fallo.getMessage());
		}
	}

}
