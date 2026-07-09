package com.pasarela.seguridad.aplicacion;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.seguridad.dominio.excepcion.UsuarioInvalidoException;
import com.pasarela.seguridad.dominio.excepcion.UsuarioYaExisteException;
import com.pasarela.seguridad.dominio.modelo.RolUsuario;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.salida.HasheadorDeContrasena;
import com.pasarela.seguridad.dominio.puerto.salida.UsuarioRepositorio;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearCuentaComercioServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T10:00:00Z");

	@Mock
	private UsuarioRepositorio repositorio;

	@Mock
	private HasheadorDeContrasena hasheador;

	private CrearCuentaComercioService servicio;

	@BeforeEach
	void configurar() {
		servicio = new CrearCuentaComercioService(
				repositorio, hasheador, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void crearCuenta_guardaUnUsuarioComercioConHash_nuncaLaContrasenaEnClaro() {
		IdComercio comercioId = IdComercio.generar();
		when(repositorio.buscarPorEmail("dueno@tienda.co")).thenReturn(Optional.empty());
		when(hasheador.hashear("secreta123")).thenReturn("$2a$10$hash");
		when(repositorio.guardar(any())).thenAnswer(returnsFirstArg());

		servicio.crearCuentaDeComercio("dueno@tienda.co", "secreta123", comercioId);

		ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
		verify(repositorio).guardar(guardado.capture());
		assertThat(guardado.getValue().rol()).isEqualTo(RolUsuario.COMERCIO);
		assertThat(guardado.getValue().comercioId()).isEqualTo(comercioId);
		assertThat(guardado.getValue().hashContrasena()).isEqualTo("$2a$10$hash");
		assertThat(guardado.getValue().creadoEn()).isEqualTo(AHORA);
	}

	@Test
	void crearCuenta_conEmailYaRegistrado_lanzaExcepcionYNoGuarda() {
		when(repositorio.buscarPorEmail("dueno@tienda.co"))
				.thenReturn(Optional.of(mock(Usuario.class)));

		assertThatThrownBy(() -> servicio.crearCuentaDeComercio(
				"dueno@tienda.co", "secreta123", IdComercio.generar()))
				.isInstanceOf(UsuarioYaExisteException.class);

		verify(repositorio, never()).guardar(any());
	}

	@Test
	void crearCuenta_conContrasenaDebil_lanzaExcepcion() {
		assertThatThrownBy(() -> servicio.crearCuentaDeComercio(
				"dueno@tienda.co", "1234567", IdComercio.generar()))
				.isInstanceOf(UsuarioInvalidoException.class)
				.hasMessageContaining("8");

		verify(repositorio, never()).guardar(any());
	}

}
