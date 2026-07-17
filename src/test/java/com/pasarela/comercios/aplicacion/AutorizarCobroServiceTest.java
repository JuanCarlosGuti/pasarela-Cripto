package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioNoAutorizadoException;
import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.excepcion.LimiteExcedidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones.RegistroDeOperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizarCobroServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T15:00:00Z");

	@Mock
	private ComercioRepositorio repositorio;

	@Mock
	private BitacoraDeOperaciones bitacora;

	private AutorizarCobroService servicio;
	private Comercio comercio;

	@BeforeEach
	void configurar() {
		servicio = new AutorizarCobroService(
				repositorio, bitacora, Clock.fixed(AHORA, ZoneOffset.UTC));
		comercio = Comercio.registrar(
				"Tienda", Nit.de("899999068-1"),
				new CuentaLiquidacion("Nequi", TipoCuenta.AHORROS, "3001234567", "Tienda"),
				AHORA.minusSeconds(3600));
	}

	@Test
	void unComercioVerificado_conCobroDentroDeLosTopes_quedaAutorizado() {
		comercio.verificar(AHORA);
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		assertThatCode(() -> servicio.autorizar(
				comercio.id(), Dinero.cop(100_000), Dinero.cop(0)))
				.doesNotThrowAnyException();
	}

	@Test
	void unComercioPendiente_noEstaAutorizado() {
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		assertThatThrownBy(() -> servicio.autorizar(
				comercio.id(), Dinero.cop(100_000), Dinero.cop(0)))
				.isInstanceOf(ComercioNoAutorizadoException.class)
				.hasMessageContaining("verificado");
	}

	@Test
	void unComercioSuspendido_noEstaAutorizado() {
		comercio.verificar(AHORA);
		comercio.suspender("actividad inusual", AHORA);
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		assertThatThrownBy(() -> servicio.autorizar(
				comercio.id(), Dinero.cop(100_000), Dinero.cop(0)))
				.isInstanceOf(ComercioNoAutorizadoException.class);
	}

	@Test
	void unCobroQueExcedeLosTopes_seRechaza_yQuedaEnLaBitacora() {
		comercio.verificar(AHORA);
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		assertThatThrownBy(() -> servicio.autorizar(
				comercio.id(), Dinero.cop(2_000_001), Dinero.cop(0)))
				.isInstanceOf(LimiteExcedidoException.class);

		ArgumentCaptor<RegistroDeOperacion> registro =
				ArgumentCaptor.forClass(RegistroDeOperacion.class);
		verify(bitacora).registrar(registro.capture());
		assertThat(registro.getValue().tipo()).isEqualTo("LIMITE_EXCEDIDO");
		assertThat(registro.getValue().actor()).isEqualTo(comercio.id().valor().toString());
		assertThat(registro.getValue().momento()).isEqualTo(AHORA);
		assertThat(registro.getValue().detalle()).contains("2000001");
	}

	@Test
	void unCobroAutorizado_noTocaLaBitacora() {
		comercio.verificar(AHORA);
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		servicio.autorizar(comercio.id(), Dinero.cop(100_000), Dinero.cop(0));

		verify(bitacora, never()).registrar(any());
	}

	@Test
	void unComercioInexistente_lanza404DeDominio() {
		when(repositorio.buscarPorId(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.autorizar(
				comercio.id(), Dinero.cop(100_000), Dinero.cop(0)))
				.isInstanceOf(ComercioNoEncontradoException.class);
	}

}
