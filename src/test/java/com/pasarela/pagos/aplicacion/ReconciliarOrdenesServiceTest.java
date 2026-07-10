package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ComandoProcesarWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ResultadoWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ReconciliarOrdenesUseCase.ResultadoReconciliacion;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroConsultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliarOrdenesServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-10T15:00:00Z");

	@Mock
	private OrdenDePagoRepositorio repositorio;

	@Mock
	private ProveedorDePagoPort proveedor;

	@Mock
	private ProcesarWebhookUseCase procesarWebhook;

	private ReconciliarOrdenesService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ReconciliarOrdenesService(repositorio, proveedor, procesarWebhook,
				Clock.fixed(AHORA, ZoneOffset.UTC), 5, "simulado");
	}

	@Test
	void buscaLasAtascadas_conElUmbralConfigurado() {
		when(repositorio.buscarPendientesCreadasAntesDe(any(), anyInt()))
				.thenReturn(List.of());

		servicio.reconciliar();

		// atascada = pendiente creada hace más de 5 minutos
		verify(repositorio).buscarPendientesCreadasAntesDe(
				eq(AHORA.minus(Duration.ofMinutes(5))), anyInt());
	}

	@Test
	void siElProveedorReportaElPago_confirmaPorLaMismaRutaDelWebhook() {
		OrdenDePago atascada = atascada();
		CobroConsultado pago = new CobroConsultado("{\"idEvento\":\"evt-recon\"}", "firma-x");
		when(repositorio.buscarPendientesCreadasAntesDe(any(), anyInt()))
				.thenReturn(List.of(atascada));
		when(proveedor.consultarCobro(atascada.referencia(), atascada.monto()))
				.thenReturn(Optional.of(pago));
		when(procesarWebhook.procesar(any())).thenReturn(ResultadoWebhook.CONFIRMADO);

		ResultadoReconciliacion resultado = servicio.reconciliar();

		assertThat(resultado.consultadas()).isEqualTo(1);
		assertThat(resultado.confirmadas()).isEqualTo(1);
		// LA MISMA ruta: el evento entra por ProcesarWebhookUseCase completo
		ArgumentCaptor<ComandoProcesarWebhook> comando =
				ArgumentCaptor.forClass(ComandoProcesarWebhook.class);
		verify(procesarWebhook).procesar(comando.capture());
		assertThat(comando.getValue().proveedor()).isEqualTo("simulado");
		assertThat(comando.getValue().cargaCruda()).isEqualTo("{\"idEvento\":\"evt-recon\"}");
		assertThat(comando.getValue().firma()).isEqualTo("firma-x");
	}

	@Test
	void siElProveedorDiceQueAunNoHayPago_noHaceNada() {
		OrdenDePago atascada = atascada();
		when(repositorio.buscarPendientesCreadasAntesDe(any(), anyInt()))
				.thenReturn(List.of(atascada));
		when(proveedor.consultarCobro(any(), any())).thenReturn(Optional.empty());

		ResultadoReconciliacion resultado = servicio.reconciliar();

		assertThat(resultado.consultadas()).isEqualTo(1);
		assertThat(resultado.confirmadas()).isZero();
		verify(procesarWebhook, never()).procesar(any());
	}

	@Test
	void siElProveedorNoResponde_registraElFalloYSigueConLasDemas() {
		OrdenDePago caida = atascada();
		OrdenDePago sana = atascada();
		CobroConsultado pago = new CobroConsultado("{}", "firma");
		when(repositorio.buscarPendientesCreadasAntesDe(any(), anyInt()))
				.thenReturn(List.of(caida, sana));
		when(proveedor.consultarCobro(caida.referencia(), caida.monto()))
				.thenThrow(new ProveedorDePagoNoDisponibleException("timeout"));
		when(proveedor.consultarCobro(sana.referencia(), sana.monto()))
				.thenReturn(Optional.of(pago));
		when(procesarWebhook.procesar(any())).thenReturn(ResultadoWebhook.CONFIRMADO);

		ResultadoReconciliacion resultado = servicio.reconciliar();

		// el fallo no tumba el ciclo ni bloquea a las demás
		assertThat(resultado.fallosDelProveedor()).isEqualTo(1);
		assertThat(resultado.confirmadas()).isEqualTo(1);
	}

	@Test
	void unDuplicadoDeLaRutaCompartida_noCuentaComoConfirmada() {
		// webhook y reconciliación casi a la vez: la idempotencia de HU-011
		// garantiza una sola confirmación — el DUPLICADO es inocuo
		OrdenDePago atascada = atascada();
		when(repositorio.buscarPendientesCreadasAntesDe(any(), anyInt()))
				.thenReturn(List.of(atascada));
		when(proveedor.consultarCobro(any(), any()))
				.thenReturn(Optional.of(new CobroConsultado("{}", "firma")));
		when(procesarWebhook.procesar(any())).thenReturn(ResultadoWebhook.DUPLICADO);

		ResultadoReconciliacion resultado = servicio.reconciliar();

		assertThat(resultado.consultadas()).isEqualTo(1);
		assertThat(resultado.confirmadas()).isZero();
	}

	private static OrdenDePago atascada() {
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				AHORA.minus(Duration.ofMinutes(10)), AHORA.plus(Duration.ofMinutes(5)));
		orden.registrarCobroEnProveedor(AHORA.minus(Duration.ofMinutes(10)));
		return orden;
	}

}
