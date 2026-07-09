package com.pasarela.seguridad.aplicacion;

import com.pasarela.seguridad.dominio.excepcion.CredencialesInvalidasException;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.entrada.AutenticarUsuarioUseCase.ComandoAutenticar;
import com.pasarela.seguridad.dominio.puerto.salida.HasheadorDeContrasena;
import com.pasarela.seguridad.dominio.puerto.salida.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioServiceTest {

	private static final String HASH = "$2a$10$hash-de-prueba";

	@Mock
	private UsuarioRepositorio repositorio;

	@Mock
	private HasheadorDeContrasena hasheador;

	private AutenticarUsuarioService servicio;
	private Usuario admin;

	@BeforeEach
	void configurar() {
		servicio = new AutenticarUsuarioService(repositorio, hasheador);
		admin = Usuario.crearAdmin("admin@pasarela.local", HASH,
				Instant.parse("2026-07-09T10:00:00Z"));
	}

	@Test
	void autenticar_conCredencialesValidas_devuelveElUsuario() {
		when(repositorio.buscarPorEmail("admin@pasarela.local")).thenReturn(Optional.of(admin));
		when(hasheador.coincide("secreta", HASH)).thenReturn(true);

		Usuario autenticado = servicio.autenticar(
				new ComandoAutenticar("Admin@Pasarela.local", "secreta"));

		assertThat(autenticado).isEqualTo(admin);
	}

	@Test
	void autenticar_conContrasenaIncorrecta_lanzaCredencialesInvalidas() {
		when(repositorio.buscarPorEmail("admin@pasarela.local")).thenReturn(Optional.of(admin));
		when(hasheador.coincide("incorrecta", HASH)).thenReturn(false);

		assertThatThrownBy(() -> servicio.autenticar(
				new ComandoAutenticar("admin@pasarela.local", "incorrecta")))
				.isInstanceOf(CredencialesInvalidasException.class)
				.hasMessage("Credenciales inválidas");
	}

	@Test
	void autenticar_conUsuarioInexistente_lanzaElMismoError_yHasheaIgual() {
		when(repositorio.buscarPorEmail("nadie@pasarela.local")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.autenticar(
				new ComandoAutenticar("nadie@pasarela.local", "loquesea")))
				.isInstanceOf(CredencialesInvalidasException.class)
				.hasMessage("Credenciales inválidas");

		// mitigación de timing: aunque el usuario no exista, se compara contra
		// un hash señuelo para que la respuesta tarde lo mismo
		verify(hasheador).coincide(eq("loquesea"), anyString());
	}

	@Test
	void autenticar_conDatosVacios_lanzaElMismoError() {
		assertThatThrownBy(() -> servicio.autenticar(new ComandoAutenticar(null, "x")))
				.isInstanceOf(CredencialesInvalidasException.class);
		assertThatThrownBy(() -> servicio.autenticar(new ComandoAutenticar("a@b.co", null)))
				.isInstanceOf(CredencialesInvalidasException.class);
	}

}
