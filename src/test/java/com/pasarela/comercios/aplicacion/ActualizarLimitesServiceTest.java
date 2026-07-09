package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.ActualizarLimitesUseCase.ComandoActualizarLimites;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualizarLimitesServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T15:00:00Z");

	@Mock
	private ComercioRepositorio repositorio;

	@Mock
	private BitacoraDeOperaciones bitacora;

	private ActualizarLimitesService servicio;
	private Comercio comercio;

	@BeforeEach
	void configurar() {
		servicio = new ActualizarLimitesService(
				repositorio, bitacora, Clock.fixed(AHORA, ZoneOffset.UTC));
		comercio = Comercio.registrar(
				"Tienda", Nit.de("899999068-1"),
				new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", "Tienda"),
				AHORA.minusSeconds(3600));
	}

	@Test
	void actualizar_cambiaLosTopes_yAuditaQuienCuandoYValores() {
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		Comercio actualizado = servicio.actualizar(new ComandoActualizarLimites(
				comercio.id().valor(), 5_000_000, 50_000_000, "admin@pasarela.local"));

		assertThat(actualizado.limites().topePorTransaccion()).isEqualTo(Dinero.cop(5_000_000));
		assertThat(actualizado.limites().topeMensual()).isEqualTo(Dinero.cop(50_000_000));

		ArgumentCaptor<RegistroDeOperacion> registro =
				ArgumentCaptor.forClass(RegistroDeOperacion.class);
		verify(bitacora).registrar(registro.capture());
		assertThat(registro.getValue().tipo()).isEqualTo("CAMBIO_DE_LIMITES");
		assertThat(registro.getValue().actor()).isEqualTo("admin@pasarela.local");
		assertThat(registro.getValue().momento()).isEqualTo(AHORA);
		// valores anterior y nuevo, auditables
		assertThat(registro.getValue().detalle())
				.contains("2000000").contains("5000000")
				.contains("20000000").contains("50000000");
	}

	@Test
	void actualizar_sobreComercioInexistente_lanza404_sinTocarLaBitacora() {
		when(repositorio.buscarPorId(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.actualizar(new ComandoActualizarLimites(
				comercio.id().valor(), 5_000_000, 50_000_000, "admin@pasarela.local")))
				.isInstanceOf(ComercioNoEncontradoException.class);

		verify(repositorio, never()).guardar(any());
		verify(bitacora, never()).registrar(any());
	}

	@Test
	void actualizar_conTopesInvalidos_propagaLaExcepcionYNoGuarda() {
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		assertThatThrownBy(() -> servicio.actualizar(new ComandoActualizarLimites(
				comercio.id().valor(), 50_000_000, 5_000_000, "admin@pasarela.local")))
				.isInstanceOf(ComercioInvalidoException.class);

		verify(repositorio, never()).guardar(any());
	}

}
