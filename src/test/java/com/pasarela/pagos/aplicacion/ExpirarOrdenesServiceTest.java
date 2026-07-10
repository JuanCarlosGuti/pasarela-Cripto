package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ExpirarOrdenesVencidasUseCase.ResultadoExpiracion;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirarOrdenesServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-10T15:00:00Z");

	@Mock
	private OrdenDePagoRepositorio repositorio;

	private ExpirarOrdenesService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ExpirarOrdenesService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void expiraLasVencidas_yLasGuarda() {
		OrdenDePago vencida = pendienteVencida();
		when(repositorio.buscarPendientesExpiradas(eq(AHORA), anyInt()))
				.thenReturn(List.of(vencida))
				.thenReturn(List.of());
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		ResultadoExpiracion resultado = servicio.expirarVencidas();

		assertThat(resultado.expiradas()).isEqualTo(1);
		assertThat(resultado.carrerasDetectadas()).isZero();
		assertThat(vencida.estado()).isEqualTo(EstadoOrden.EXPIRADA);
		verify(repositorio).guardar(vencida);
	}

	@Test
	void sinVencidas_noHaceNada() {
		when(repositorio.buscarPendientesExpiradas(eq(AHORA), anyInt()))
				.thenReturn(List.of());

		ResultadoExpiracion resultado = servicio.expirarVencidas();

		assertThat(resultado.expiradas()).isZero();
		verify(repositorio, never()).guardar(any());
	}

	@Test
	void siPierdeLaCarreraContraUnPago_laSaltaLimpiamente_ySigueConLasDemas() {
		OrdenDePago perdedora = pendienteVencida();
		OrdenDePago normal = pendienteVencida();
		when(repositorio.buscarPendientesExpiradas(eq(AHORA), anyInt()))
				.thenReturn(List.of(perdedora, normal))
				.thenReturn(List.of());
		when(repositorio.guardar(perdedora))
				.thenThrow(new OptimisticLockingFailureException("el pago ganó"));
		when(repositorio.guardar(normal)).thenAnswer(returnsFirstArg());

		ResultadoExpiracion resultado = servicio.expirarVencidas();

		assertThat(resultado.expiradas()).isEqualTo(1);
		assertThat(resultado.carrerasDetectadas()).isEqualTo(1);
		assertThat(normal.estado()).isEqualTo(EstadoOrden.EXPIRADA);
	}

	@Test
	void procesaPorLotes_hastaVaciarLaCola() {
		List<OrdenDePago> loteLleno = IntStream.range(0, 100)
				.mapToObj(i -> pendienteVencida()).toList();
		OrdenDePago rezagada = pendienteVencida();
		when(repositorio.buscarPendientesExpiradas(eq(AHORA), anyInt()))
				.thenReturn(loteLleno)
				.thenReturn(List.of(rezagada))
				.thenReturn(List.of());
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		ResultadoExpiracion resultado = servicio.expirarVencidas();

		assertThat(resultado.expiradas()).isEqualTo(101);
		// lote lleno → vuelve a consultar; lote corto → termina
		verify(repositorio, times(2)).buscarPendientesExpiradas(eq(AHORA), anyInt());
	}

	@Test
	void esIdempotente_correrloDosVeces_produceLoMismo() {
		OrdenDePago vencida = pendienteVencida();
		when(repositorio.buscarPendientesExpiradas(eq(AHORA), anyInt()))
				.thenReturn(List.of(vencida))
				.thenReturn(List.of())
				.thenReturn(List.of()); // segunda corrida: ya no hay pendientes
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		ResultadoExpiracion primera = servicio.expirarVencidas();
		ResultadoExpiracion segunda = servicio.expirarVencidas();

		assertThat(primera.expiradas()).isEqualTo(1);
		assertThat(segunda.expiradas()).isZero();
		verify(repositorio, times(1)).guardar(any());
	}

	private static OrdenDePago pendienteVencida() {
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				AHORA.minus(Duration.ofMinutes(30)), AHORA.minus(Duration.ofMinutes(15)));
		orden.registrarCobroEnProveedor(AHORA.minus(Duration.ofMinutes(30)));
		return orden;
	}

}
