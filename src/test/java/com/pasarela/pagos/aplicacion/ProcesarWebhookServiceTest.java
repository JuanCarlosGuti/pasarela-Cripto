package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import com.pasarela.pagos.dominio.excepcion.FirmaDeWebhookInvalidaException;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.EventoProveedor;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ComandoProcesarWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ResultadoWebhook;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio.EventoDuplicadoException;
import com.pasarela.pagos.dominio.puerto.salida.NotificadorDeComercios;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.WebhookDelProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesarWebhookServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T15:05:00Z");
	private static final String CARGA = "{\"idEvento\":\"evt-1\"}";
	private static final ComandoProcesarWebhook COMANDO =
			new ComandoProcesarWebhook("simulado", CARGA, "firma-ok");

	@Mock
	private EventoProveedorRepositorio eventos;

	@Mock
	private OrdenDePagoRepositorio ordenes;

	@Mock
	private ProveedorDePagoPort proveedor;

	@Mock
	private NotificadorDeComercios notificador;

	@Mock
	private BitacoraDeOperaciones bitacora;

	private ProcesarWebhookService servicio;
	private OrdenDePago orden;
	private WebhookDelProveedor webhook;

	@BeforeEach
	void configurar() {
		servicio = new ProcesarWebhookService(eventos, ordenes, proveedor, notificador,
				bitacora, Clock.fixed(AHORA, ZoneOffset.UTC));
		orden = OrdenDePago.crear(IdComercio.generar(), Dinero.cop(40000),
				ReferenciaPago.generar(), AHORA.minusSeconds(120),
				AHORA.plus(Duration.ofMinutes(13)));
		orden.registrarCobroEnProveedor(AHORA.minusSeconds(120));
		webhook = new WebhookDelProveedor("evt-1", "PAGO_RECIBIDO",
				orden.referencia(), Dinero.cop(40000), AHORA.minusSeconds(5));
	}

	@Test
	void caminoFeliz_sigueExactamenteElOrdenDelFlujo() {
		when(proveedor.firmaValida(CARGA, "firma-ok")).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe("simulado", "evt-1")).thenReturn(false);
		// el evento es mutable: registrar su estado EN el momento de cada guardado
		java.util.List<Boolean> procesadoAlGuardar = new java.util.ArrayList<>();
		when(eventos.guardar(any())).thenAnswer(invocacion -> {
			EventoProveedor guardado = invocacion.getArgument(0);
			procesadoAlGuardar.add(guardado.procesado());
			return guardado;
		});
		when(ordenes.buscarPorReferencia(orden.referencia())).thenReturn(Optional.of(orden));
		when(ordenes.guardar(any())).thenAnswer(returnsFirstArg());

		ResultadoWebhook resultado = servicio.procesar(COMANDO);

		assertThat(resultado).isEqualTo(ResultadoWebhook.CONFIRMADO);
		assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);

		// el orden EXACTO de docs/05: firma → crudo → idempotencia → confirmar → notificar
		InOrder orderVerifier = inOrder(proveedor, eventos, ordenes, notificador);
		orderVerifier.verify(proveedor).firmaValida(CARGA, "firma-ok");
		orderVerifier.verify(proveedor).interpretarWebhook(CARGA);
		orderVerifier.verify(eventos).existe("simulado", "evt-1");
		orderVerifier.verify(eventos).guardar(any());
		orderVerifier.verify(ordenes).buscarPorReferencia(orden.referencia());
		orderVerifier.verify(ordenes).guardar(orden);
		orderVerifier.verify(notificador).pagoDetectado(orden);
		orderVerifier.verify(eventos).guardar(any()); // marcado procesado, al final

		// el crudo se guardó SIN procesar (antes de confirmar) y luego procesado
		assertThat(procesadoAlGuardar).containsExactly(false, true);
	}

	@Test
	void firmaInvalida_registraElIntento_lanza401_yNoTocaNadaMas() {
		when(proveedor.firmaValida(CARGA, "firma-ok")).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());

		assertThatThrownBy(() -> servicio.procesar(COMANDO))
				.isInstanceOf(FirmaDeWebhookInvalidaException.class);

		ArgumentCaptor<EventoProveedor> intento = ArgumentCaptor.forClass(EventoProveedor.class);
		verify(eventos).guardar(intento.capture());
		assertThat(intento.getValue().firmaValida()).isFalse();
		verifyNoInteractions(ordenes, notificador);
	}

	@Test
	void eventoYaVisto_devuelveDuplicado_sinNingunEfecto() {
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe("simulado", "evt-1")).thenReturn(true);

		assertThat(servicio.procesar(COMANDO)).isEqualTo(ResultadoWebhook.DUPLICADO);

		verify(eventos, never()).guardar(any());
		verifyNoInteractions(ordenes, notificador);
	}

	@Test
	void carreraDetectadaPorLaConstraint_tambienEsDuplicado() {
		// la verificación de existencia pasó, pero otro hilo insertó primero:
		// la constraint única es la última línea de defensa (ADR-004)
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe("simulado", "evt-1")).thenReturn(false);
		when(eventos.guardar(any())).thenThrow(new EventoDuplicadoException("duplicado"));

		assertThat(servicio.procesar(COMANDO)).isEqualTo(ResultadoWebhook.DUPLICADO);

		verifyNoInteractions(ordenes, notificador);
	}

	@Test
	void ordenInexistente_dejaElEventoParaRevision_yAlertaEnBitacora() {
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any())).thenReturn(Optional.empty());

		assertThat(servicio.procesar(COMANDO)).isEqualTo(ResultadoWebhook.PARA_REVISION);

		ArgumentCaptor<EventoProveedor> guardados = ArgumentCaptor.forClass(EventoProveedor.class);
		verify(eventos, times(2)).guardar(guardados.capture());
		assertThat(guardados.getAllValues().get(1).notaRevision()).isNotBlank();
		assertThat(guardados.getAllValues().get(1).procesado()).isFalse();
		verify(bitacora).registrar(any());
		verify(ordenes, never()).guardar(any());
		verifyNoInteractions(notificador);
	}

	@Test
	void montoDistintoAlEsperado_llevaLaOrdenARevision_yAlertaEnBitacora() {
		WebhookDelProveedor montoErrado = new WebhookDelProveedor("evt-1", "PAGO_RECIBIDO",
				orden.referencia(), Dinero.cop(99999), AHORA.minusSeconds(5));
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(montoErrado);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any())).thenReturn(Optional.of(orden));
		when(ordenes.guardar(any())).thenAnswer(returnsFirstArg());

		ResultadoWebhook resultado = servicio.procesar(COMANDO);

		assertThat(resultado).isEqualTo(ResultadoWebhook.PARA_REVISION);
		assertThat(orden.estado()).isEqualTo(EstadoOrden.EN_REVISION);
		// el motivo queda auditado en el historial: PENDIENTE→FALLIDA con monto
		assertThat(orden.historial().getLast().desde()).isEqualTo(EstadoOrden.FALLIDA);
		assertThat(orden.historial().get(orden.historial().size() - 2).motivo())
				.contains("99999").contains("40000");
		verify(ordenes).guardar(orden);
		verify(bitacora).registrar(any());
		verifyNoInteractions(notificador);
	}

	@Test
	void pagoTardio_sobreOrdenYaExpirada_noConfirma_yQuedaParaRevision() {
		OrdenDePago expirada = OrdenDePago.crear(IdComercio.generar(), Dinero.cop(40000),
				webhook.referencia(), AHORA.minusSeconds(1200), AHORA.minusSeconds(300));
		expirada.registrarCobroEnProveedor(AHORA.minusSeconds(1200));
		expirada.expirar(AHORA);
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any())).thenReturn(Optional.of(expirada));

		ResultadoWebhook resultado = servicio.procesar(COMANDO);

		assertThat(resultado).isEqualTo(ResultadoWebhook.PARA_REVISION);
		assertThat(expirada.estado()).isEqualTo(EstadoOrden.EXPIRADA); // intacta
		verify(ordenes, never()).guardar(any());
		verify(bitacora).registrar(any());
		verifyNoInteractions(notificador);
	}

	@Test
	void tipoDeEventoNoSoportado_oFueraDeOrden_quedaParaRevision_sinCorromperNada() {
		WebhookDelProveedor fueraDeOrden = new WebhookDelProveedor("evt-1", "PAGO_CONVERTIDO",
				orden.referencia(), Dinero.cop(40000), AHORA.minusSeconds(5));
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(fueraDeOrden);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any())).thenReturn(Optional.of(orden));

		ResultadoWebhook resultado = servicio.procesar(COMANDO);

		assertThat(resultado).isEqualTo(ResultadoWebhook.PARA_REVISION);
		assertThat(orden.estado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO); // intacta
		verify(ordenes, never()).guardar(any());
		verifyNoInteractions(notificador);
	}

	@Test
	void siLaExpiracionGanaLaCarrera_elWebhookRecarga_yTerminaEnPagoTardio() {
		// el guardado pierde contra el job de expiración (HU-014): se recarga
		// la orden y se decide de nuevo por el camino normal → PAGO_TARDIO
		OrdenDePago expirada = OrdenDePago.crear(IdComercio.generar(), Dinero.cop(40000),
				webhook.referencia(), AHORA.minusSeconds(1200), AHORA.minusSeconds(300));
		expirada.registrarCobroEnProveedor(AHORA.minusSeconds(1200));
		expirada.expirar(AHORA);
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any()))
				.thenReturn(Optional.of(orden))      // primera lectura: aún pendiente
				.thenReturn(Optional.of(expirada));  // recarga tras la carrera
		when(ordenes.guardar(orden))
				.thenThrow(new org.springframework.dao.OptimisticLockingFailureException(
						"la expiración ganó"));

		ResultadoWebhook resultado = servicio.procesar(COMANDO);

		assertThat(resultado).isEqualTo(ResultadoWebhook.PARA_REVISION);
		verify(ordenes, times(2)).buscarPorReferencia(webhook.referencia());
		verify(bitacora).registrar(any());
		verifyNoInteractions(notificador);
	}

	@Test
	void siLaOrdenDesapareceDuranteLaCarrera_falla_ruidosamente() {
		// jamás debería pasar (las órdenes no se borran); si pasa, mejor un
		// error ruidoso que un estado silenciosamente inconsistente
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any()))
				.thenReturn(Optional.of(orden))
				.thenReturn(Optional.empty());
		when(ordenes.guardar(orden))
				.thenThrow(new org.springframework.dao.OptimisticLockingFailureException("carrera"));

		assertThatThrownBy(() -> servicio.procesar(COMANDO))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("desapareció");
	}

	@Test
	void siLaNotificacionFalla_laConfirmacionNoSeRevierte() {
		// HU-013: la notificación es best-effort; la fuente de verdad es el estado
		when(proveedor.firmaValida(any(), any())).thenReturn(true);
		when(proveedor.interpretarWebhook(CARGA)).thenReturn(webhook);
		when(eventos.existe(any(), any())).thenReturn(false);
		when(eventos.guardar(any())).thenAnswer(returnsFirstArg());
		when(ordenes.buscarPorReferencia(any())).thenReturn(Optional.of(orden));
		when(ordenes.guardar(any())).thenAnswer(returnsFirstArg());
		doThrow(new RuntimeException("notificador caído"))
				.when(notificador).pagoDetectado(any());

		ResultadoWebhook resultado = servicio.procesar(COMANDO);

		assertThat(resultado).isEqualTo(ResultadoWebhook.CONFIRMADO);
		assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
		verify(ordenes).guardar(orden);
		verify(eventos, times(2)).guardar(any()); // el evento igual queda procesado
	}

}
