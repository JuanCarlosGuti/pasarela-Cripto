package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.pagos.dominio.excepcion.ComprobanteNoDisponibleException;
import com.pasarela.pagos.dominio.excepcion.OrdenNoEncontradaException;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.EventoPago;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase.ComandoConsultarOrden;
import com.pasarela.pagos.dominio.puerto.entrada.GenerarComprobanteUseCase.ComandoGenerarComprobante;
import com.pasarela.pagos.dominio.puerto.entrada.GenerarComprobanteUseCase.Comprobante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Comprobante de venta (HU-020): solo las órdenes con pago detectado o
 * posterior lo tienen; el aislamiento entre comercios se hereda de la
 * consulta de la orden (HU-009).
 */
@ExtendWith(MockitoExtension.class)
class GenerarComprobanteServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-13T15:00:00Z");
	private static final Instant CREADA_EN = Instant.parse("2026-07-13T10:00:00Z");
	private static final Instant EXPIRA_EN = CREADA_EN.plus(Duration.ofMinutes(15));
	private static final Instant REGISTRADA_EN = CREADA_EN.plusSeconds(5);
	private static final Instant PAGADA_EN = CREADA_EN.plusSeconds(90);
	private static final Instant CONVERTIDA_EN = CREADA_EN.plusSeconds(120);
	private static final Instant LIQUIDADA_EN = CREADA_EN.plusSeconds(180);

	@Mock
	private ConsultarOrdenUseCase consultarOrden;

	private GenerarComprobanteService servicio;

	@BeforeEach
	void configurar() {
		servicio = new GenerarComprobanteService(
				consultarOrden, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void unaOrdenPagada_tieneComprobanteConLosDatosDelCobro() {
		OrdenDePago orden = ordenPagada();
		UUID comercioId = orden.comercioId().valor();
		when(consultarOrden.consultar(
				new ComandoConsultarOrden(orden.id().valor(), comercioId)))
				.thenReturn(orden);

		Comprobante comprobante = servicio.generar(
				new ComandoGenerarComprobante(orden.id().valor(), comercioId));

		assertThat(comprobante.numero()).isEqualTo(orden.id().valor());
		assertThat(comprobante.referencia()).isEqualTo(orden.referencia().valor());
		assertThat(comprobante.monto()).isEqualTo(Dinero.cop(40000));
		assertThat(comprobante.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
		assertThat(comprobante.creadaEn()).isEqualTo(CREADA_EN);
		assertThat(comprobante.pagoDetectadoEn()).isEqualTo(PAGADA_EN);
		assertThat(comprobante.liquidadaEn()).isNull(); // aún no liquidada
		assertThat(comprobante.emitidoEn()).isEqualTo(AHORA);
	}

	@Test
	void unaOrdenLiquidada_incluyeElMomentoDeLaLiquidacion() {
		OrdenDePago orden = ordenPagada();
		orden.marcarComoConvertida(CONVERTIDA_EN);
		orden.marcarComoLiquidada(LIQUIDADA_EN);
		when(consultarOrden.consultar(any())).thenReturn(orden);

		Comprobante comprobante = servicio.generar(
				new ComandoGenerarComprobante(orden.id().valor(), orden.comercioId().valor()));

		assertThat(comprobante.estado()).isEqualTo(EstadoOrden.LIQUIDADA);
		assertThat(comprobante.pagoDetectadoEn()).isEqualTo(PAGADA_EN);
		assertThat(comprobante.liquidadaEn()).isEqualTo(LIQUIDADA_EN);
	}

	@Test
	void unaOrdenSinPagoDetectado_noTieneComprobante() {
		OrdenDePago pendiente = ordenPendiente();
		when(consultarOrden.consultar(any())).thenReturn(pendiente);
		assertThatThrownBy(() -> servicio.generar(
				new ComandoGenerarComprobante(pendiente.id().valor(), pendiente.comercioId().valor())))
				.isInstanceOf(ComprobanteNoDisponibleException.class)
				.hasMessageContaining("PENDIENTE_PAGO");

		OrdenDePago expirada = ordenPendiente();
		expirada.expirar(EXPIRA_EN.plusSeconds(1));
		when(consultarOrden.consultar(any())).thenReturn(expirada);
		assertThatThrownBy(() -> servicio.generar(
				new ComandoGenerarComprobante(expirada.id().valor(), expirada.comercioId().valor())))
				.isInstanceOf(ComprobanteNoDisponibleException.class)
				.hasMessageContaining("EXPIRADA");
	}

	@Test
	void unaOrdenAjenaOInexistente_respondeIgualQueNoEncontrada() {
		// el aislamiento de HU-009 se hereda tal cual: la excepción atraviesa
		when(consultarOrden.consultar(any()))
				.thenThrow(new OrdenNoEncontradaException("No existe una orden con ese id"));

		assertThatThrownBy(() -> servicio.generar(
				new ComandoGenerarComprobante(UUID.randomUUID(), UUID.randomUUID())))
				.isInstanceOf(OrdenNoEncontradaException.class);
	}

	@Test
	void unaOrdenPagadaSinLaTransicionEnElHistorial_delataElDatoCorrupto() {
		// reconstituir no re-valida coherencia estado-historial: si la BD trae
		// una pagada sin rastro del pago, mejor estallar que emitir un
		// comprobante sin fecha de pago
		OrdenDePago corrupta = OrdenDePago.reconstituir(
				IdOrden.generar(), IdComercio.generar(), Dinero.cop(40000),
				ReferenciaPago.generar(), CREADA_EN, EXPIRA_EN,
				EstadoOrden.PAGO_DETECTADO, List.of(), 0L);
		when(consultarOrden.consultar(any())).thenReturn(corrupta);

		assertThatThrownBy(() -> servicio.generar(
				new ComandoGenerarComprobante(corrupta.id().valor(), corrupta.comercioId().valor())))
				.isInstanceOf(IllegalStateException.class);
	}

	// --- ayudas ---

	private static OrdenDePago ordenPendiente() {
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				CREADA_EN, EXPIRA_EN);
		orden.registrarCobroEnProveedor(REGISTRADA_EN);
		return orden;
	}

	private static OrdenDePago ordenPagada() {
		OrdenDePago orden = ordenPendiente();
		orden.confirmarPago(
				new EventoPago(orden.referencia(), orden.monto(), PAGADA_EN), PAGADA_EN);
		return orden;
	}

}
