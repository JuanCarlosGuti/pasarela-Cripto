package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.ComercioYaRegistradoException;
import com.pasarela.comercios.dominio.excepcion.NitInvalidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase.ComandoRegistrarComercio;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.puerto.CuentasDeAccesoPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class RegistrarComercioServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-08T10:00:00Z");
	private static final ComandoRegistrarComercio COMANDO = new ComandoRegistrarComercio(
			"Tienda La Esquina SAS", "899999068-1", "NEQUI", "3001234567",
			"Tienda La Esquina SAS", "dueno@tienda.co", "secreta123");

	@Mock
	private ComercioRepositorio repositorio;

	@Mock
	private CuentasDeAccesoPort cuentasDeAcceso;

	private RegistrarComercioService servicio;

	@BeforeEach
	void configurar() {
		servicio = new RegistrarComercioService(
				repositorio, cuentasDeAcceso, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void registrar_conDatosValidos_guardaElComercioPendiente() {
		when(repositorio.buscarPorNit(any())).thenReturn(Optional.empty());
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		Comercio registrado = servicio.registrar(COMANDO);

		ArgumentCaptor<Comercio> guardado = ArgumentCaptor.forClass(Comercio.class);
		verify(repositorio).guardar(guardado.capture());
		assertThat(guardado.getValue().nit()).isEqualTo(Nit.de("899999068-1"));
		assertThat(guardado.getValue().estadoVerificacion())
				.isEqualTo(EstadoVerificacion.PENDIENTE);
		assertThat(guardado.getValue().cuentaLiquidacion().tipo()).isEqualTo(TipoCuenta.NEQUI);
		assertThat(guardado.getValue().registradoEn()).isEqualTo(AHORA);
		assertThat(registrado.puedeCobrar()).isFalse();
	}

	@Test
	void registrar_creaLaCuentaDeAccesoDelDueno_conElIdDelComercioGuardado() {
		when(repositorio.buscarPorNit(any())).thenReturn(Optional.empty());
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		Comercio registrado = servicio.registrar(COMANDO);

		verify(cuentasDeAcceso).crearCuentaDeComercio(
				"dueno@tienda.co", "secreta123", registrado.id());
	}

	@Test
	void registrar_conNitYaRegistrado_lanzaExcepcionYNoGuardaNada() {
		Comercio existente = mock(Comercio.class);
		when(repositorio.buscarPorNit(Nit.de("899999068-1")))
				.thenReturn(Optional.of(existente));

		assertThatThrownBy(() -> servicio.registrar(COMANDO))
				.isInstanceOf(ComercioYaRegistradoException.class);

		verify(repositorio, never()).guardar(any());
	}

	@Test
	void registrar_conNitInvalido_propagaLaExcepcionDeDominio_sinTocarElRepositorio() {
		ComandoRegistrarComercio comandoInvalido = new ComandoRegistrarComercio(
				"Tienda", "899999068-2", "NEQUI", "3001234567", "Tienda",
				"dueno@tienda.co", "secreta123");

		assertThatThrownBy(() -> servicio.registrar(comandoInvalido))
				.isInstanceOf(NitInvalidoException.class);

		verify(repositorio, never()).guardar(any());
	}

	@Test
	void registrar_conTipoDeCuentaDesconocido_lanzaExcepcionClara() {
		ComandoRegistrarComercio comandoInvalido = new ComandoRegistrarComercio(
				"Tienda", "899999068-1", "BILLETERA_MAGICA", "3001234567", "Tienda",
				"dueno@tienda.co", "secreta123");

		assertThatThrownBy(() -> servicio.registrar(comandoInvalido))
				.isInstanceOf(ComercioInvalidoException.class)
				.hasMessageContaining("tipo de cuenta");
	}

}