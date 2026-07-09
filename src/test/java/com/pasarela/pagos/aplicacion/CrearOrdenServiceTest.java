package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.excepcion.MontoInvalidoException;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.AutorizadorDeCobros;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.ComandoCrearOrden;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.OrdenCreada;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroCreado;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.SolicitudDeCobro;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearOrdenServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T15:00:00Z");
	private static final UUID COMERCIO_ID = UUID.randomUUID();
	private static final CobroCreado COBRO = new CobroCreado(
			"PAGOSIM|ref|40000", "pasarela-sim://pagar/ref");

	@Mock
	private OrdenDePagoRepositorio repositorio;

	@Mock
	private ProveedorDePagoPort proveedor;

	@Mock
	private AutorizadorDeCobros autorizador;

	private CrearOrdenService servicio;

	@BeforeEach
	void configurar() {
		servicio = new CrearOrdenService(repositorio, proveedor, autorizador,
				Clock.fixed(AHORA, ZoneOffset.UTC), 15);
	}

	@Test
	void crear_conComercioAutorizado_dejaLaOrdenPendienteDePagoConVentanaDe15Minutos() {
		when(repositorio.acumuladoDelMes(any(), any(), any())).thenReturn(Dinero.cop(0));
		when(proveedor.crearCobro(any())).thenReturn(COBRO);
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		OrdenCreada resultado = servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000));

		assertThat(resultado.orden().estado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
		assertThat(resultado.orden().comercioId()).isEqualTo(IdComercio.de(COMERCIO_ID));
		assertThat(resultado.orden().monto()).isEqualTo(Dinero.cop(40000));
		assertThat(resultado.orden().creadaEn()).isEqualTo(AHORA);
		assertThat(resultado.orden().expiraEn())
				.isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
		assertThat(resultado.cobro()).isEqualTo(COBRO);
	}

	@Test
	void crear_llamaAlProveedorConLaReferenciaYExpiracionDeLaOrden() {
		when(repositorio.acumuladoDelMes(any(), any(), any())).thenReturn(Dinero.cop(0));
		when(proveedor.crearCobro(any())).thenReturn(COBRO);
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		OrdenCreada resultado = servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000));

		ArgumentCaptor<SolicitudDeCobro> solicitud =
				ArgumentCaptor.forClass(SolicitudDeCobro.class);
		verify(proveedor).crearCobro(solicitud.capture());
		assertThat(solicitud.getValue().referencia()).isEqualTo(resultado.orden().referencia());
		assertThat(solicitud.getValue().monto()).isEqualTo(Dinero.cop(40000));
		assertThat(solicitud.getValue().expiraEn()).isEqualTo(resultado.orden().expiraEn());
	}

	@Test
	void crear_pasaElAcumuladoDelMesAlAutorizador() {
		when(repositorio.acumuladoDelMes(eq(IdComercio.de(COMERCIO_ID)), any(), any()))
				.thenReturn(Dinero.cop(5_000_000));
		when(proveedor.crearCobro(any())).thenReturn(COBRO);
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000));

		verify(autorizador).autorizar(
				IdComercio.de(COMERCIO_ID), Dinero.cop(40000), Dinero.cop(5_000_000));
	}

	@Test
	void siElAutorizadorRechaza_noSeLlamaAlProveedorNiSePersisteNada() {
		when(repositorio.acumuladoDelMes(any(), any(), any())).thenReturn(Dinero.cop(0));
		org.mockito.Mockito.doThrow(new RuntimeException("no autorizado"))
				.when(autorizador).autorizar(any(), any(), any());

		assertThatThrownBy(() -> servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000)))
				.isInstanceOf(RuntimeException.class);

		verifyNoInteractions(proveedor);
		verify(repositorio, never()).guardar(any());
	}

	@Test
	void siElProveedorFalla_noQuedaNingunaOrdenPersistida() {
		when(repositorio.acumuladoDelMes(any(), any(), any())).thenReturn(Dinero.cop(0));
		when(proveedor.crearCobro(any()))
				.thenThrow(new ProveedorDePagoNoDisponibleException("proveedor caído"));

		assertThatThrownBy(() -> servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000)))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class);

		verify(repositorio, never()).guardar(any());
	}

	@Test
	void crear_conMontoCero_falla_sinTocarProveedorNiAutorizador() {
		assertThatThrownBy(() -> servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 0)))
				.isInstanceOf(OrdenInvalidaException.class)
				.hasMessageContaining("mayor que cero");

		verifyNoInteractions(proveedor, autorizador);
		verify(repositorio, never()).guardar(any());
	}

	@Test
	void crear_conMontoNegativo_falla_sinTocarNada() {
		assertThatThrownBy(() -> servicio.crear(new ComandoCrearOrden(COMERCIO_ID, -5)))
				.isInstanceOf(MontoInvalidoException.class);

		verifyNoInteractions(proveedor, autorizador);
	}

	@Test
	void crear_consultaElAcumuladoDelMesCalendarioEnZonaColombia() {
		// 2026-07-09T15:00Z = 10:00 en Bogotá → mes de julio en UTC-5:
		// [2026-07-01T05:00Z, 2026-08-01T05:00Z)
		when(repositorio.acumuladoDelMes(any(), any(), any())).thenReturn(Dinero.cop(0));
		when(proveedor.crearCobro(any())).thenReturn(COBRO);
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000));

		verify(repositorio).acumuladoDelMes(
				IdComercio.de(COMERCIO_ID),
				Instant.parse("2026-07-01T05:00:00Z"),
				Instant.parse("2026-08-01T05:00:00Z"));
	}

	@Test
	void laOrdenGuardada_esLaQueSeDevuelve() {
		when(repositorio.acumuladoDelMes(any(), any(), any())).thenReturn(Dinero.cop(0));
		when(proveedor.crearCobro(any())).thenReturn(COBRO);
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		OrdenCreada resultado = servicio.crear(new ComandoCrearOrden(COMERCIO_ID, 40000));

		ArgumentCaptor<OrdenDePago> guardada = ArgumentCaptor.forClass(OrdenDePago.class);
		verify(repositorio).guardar(guardada.capture());
		assertThat(resultado.orden()).isEqualTo(guardada.getValue());
		assertThat(guardada.getValue().historial()).hasSize(1); // CREADA → PENDIENTE_PAGO
	}

}
