package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.excepcion.VerificacionInvalidaException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase.ComandoDecisionVerificacion;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecidirVerificacionServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-08T15:00:00Z");

	@Mock
	private ComercioRepositorio repositorio;

	private DecidirVerificacionService servicio;
	private Comercio pendiente;

	@BeforeEach
	void configurar() {
		servicio = new DecidirVerificacionService(
				repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
		pendiente = Comercio.registrar(
				"Tienda", Nit.de("899999068-1"),
				new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", "Tienda"),
				AHORA.minusSeconds(3600));
	}

	@Test
	void aprobar_unComercioPendiente_loDejaVerificadoYLoGuarda() {
		when(repositorio.buscarPorId(pendiente.id())).thenReturn(Optional.of(pendiente));
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		Comercio decidido = servicio.decidir(comando("APROBAR", null));

		assertThat(decidido.estadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADO);
		assertThat(decidido.puedeCobrar()).isTrue();
		assertThat(decidido.decisionEn()).isEqualTo(AHORA);
		verify(repositorio).guardar(pendiente);
	}

	@Test
	void rechazar_conMotivo_loDejaRechazadoConElMotivo() {
		when(repositorio.buscarPorId(pendiente.id())).thenReturn(Optional.of(pendiente));
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		Comercio decidido = servicio.decidir(
				comando("RECHAZAR", "documentos inconsistentes"));

		assertThat(decidido.estadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADO);
		assertThat(decidido.motivoDecision()).isEqualTo("documentos inconsistentes");
	}

	@Test
	void decidir_sobreComercioInexistente_lanza404DeDominio() {
		when(repositorio.buscarPorId(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.decidir(comando("APROBAR", null)))
				.isInstanceOf(ComercioNoEncontradoException.class);

		verify(repositorio, never()).guardar(any());
	}

	@Test
	void unaDecisionInvalidaParaElEstado_propagaLaExcepcionYNoGuarda() {
		when(repositorio.buscarPorId(pendiente.id())).thenReturn(Optional.of(pendiente));

		assertThatThrownBy(() -> servicio.decidir(comando("SUSPENDER", "actividad inusual")))
				.isInstanceOf(VerificacionInvalidaException.class);

		verify(repositorio, never()).guardar(any());
	}

	@Test
	void unaDecisionDesconocida_lanzaExcepcionClara() {
		when(repositorio.buscarPorId(pendiente.id())).thenReturn(Optional.of(pendiente));

		assertThatThrownBy(() -> servicio.decidir(comando("CONGELAR", null)))
				.isInstanceOf(ComercioInvalidoException.class)
				.hasMessageContaining("decisión");
	}

	private ComandoDecisionVerificacion comando(String decision, String motivo) {
		return new ComandoDecisionVerificacion(pendiente.id().valor(), decision, motivo);
	}

}