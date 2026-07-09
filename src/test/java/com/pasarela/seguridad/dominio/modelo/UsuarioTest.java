package com.pasarela.seguridad.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.seguridad.dominio.excepcion.UsuarioInvalidoException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T10:00:00Z");
	private static final String HASH = "$2a$10$hash-de-prueba-no-real-abcdefghijklmnopqrstuv";

	@Nested
	class Creacion {

		@Test
		void crearAdmin_quedaConRolAdmin_sinComercio() {
			Usuario admin = Usuario.crearAdmin("Admin@Pasarela.local", HASH, AHORA);

			assertThat(admin.id()).isNotNull();
			assertThat(admin.email()).isEqualTo("admin@pasarela.local"); // normalizado
			assertThat(admin.rol()).isEqualTo(RolUsuario.ADMIN);
			assertThat(admin.comercioId()).isNull();
			assertThat(admin.creadoEn()).isEqualTo(AHORA);
		}

		@Test
		void crearComercio_quedaConRolComercio_yComercioAsociado() {
			IdComercio comercioId = IdComercio.generar();

			Usuario usuario = Usuario.crearComercio("dueno@tienda.co", HASH, comercioId, AHORA);

			assertThat(usuario.rol()).isEqualTo(RolUsuario.COMERCIO);
			assertThat(usuario.comercioId()).isEqualTo(comercioId);
		}

		@Test
		void crearComercio_sinComercioAsociado_lanzaExcepcion() {
			assertThatThrownBy(() -> Usuario.crearComercio("dueno@tienda.co", HASH, null, AHORA))
					.isInstanceOf(UsuarioInvalidoException.class)
					.hasMessageContaining("comercio");
		}

		@Test
		void crear_conEmailInvalido_lanzaExcepcion() {
			assertThatThrownBy(() -> Usuario.crearAdmin("sin-arroba", HASH, AHORA))
					.isInstanceOf(UsuarioInvalidoException.class)
					.hasMessageContaining("email");
			assertThatThrownBy(() -> Usuario.crearAdmin("  ", HASH, AHORA))
					.isInstanceOf(UsuarioInvalidoException.class);
			assertThatThrownBy(() -> Usuario.crearAdmin(null, HASH, AHORA))
					.isInstanceOf(UsuarioInvalidoException.class);
		}

		@Test
		void crear_sinHashOSinFecha_lanzaExcepcion() {
			assertThatThrownBy(() -> Usuario.crearAdmin("a@b.co", " ", AHORA))
					.isInstanceOf(UsuarioInvalidoException.class);
			assertThatThrownBy(() -> Usuario.crearAdmin("a@b.co", null, AHORA))
					.isInstanceOf(UsuarioInvalidoException.class);
			assertThatThrownBy(() -> Usuario.crearAdmin("a@b.co", HASH, null))
					.isInstanceOf(UsuarioInvalidoException.class);
		}
	}

	@Nested
	class Reconstitucion {

		@Test
		void reconstituir_restauraTalCual() {
			Usuario original = Usuario.crearComercio(
					"dueno@tienda.co", HASH, IdComercio.generar(), AHORA);

			Usuario reconstituido = Usuario.reconstituir(
					original.id(), original.email(), original.hashContrasena(),
					original.rol(), original.comercioId(), original.creadoEn());

			assertThat(reconstituido.id()).isEqualTo(original.id());
			assertThat(reconstituido.email()).isEqualTo(original.email());
			assertThat(reconstituido.rol()).isEqualTo(original.rol());
			assertThat(reconstituido.comercioId()).isEqualTo(original.comercioId());
		}

		@Test
		void reconstituir_admin_noRequiereComercio() {
			Usuario original = Usuario.crearAdmin("admin@pasarela.local", HASH, AHORA);

			Usuario reconstituido = Usuario.reconstituir(
					original.id(), original.email(), original.hashContrasena(),
					original.rol(), null, original.creadoEn());

			assertThat(reconstituido.rol()).isEqualTo(RolUsuario.ADMIN);
			assertThat(reconstituido.comercioId()).isNull();
		}

		@Test
		void reconstituir_comercioSinComercioId_lanzaExcepcion() {
			Usuario original = Usuario.crearComercio(
					"dueno@tienda.co", HASH, IdComercio.generar(), AHORA);

			assertThatThrownBy(() -> Usuario.reconstituir(
					original.id(), original.email(), original.hashContrasena(),
					RolUsuario.COMERCIO, null, original.creadoEn()))
					.isInstanceOf(UsuarioInvalidoException.class);
		}
	}

	@Nested
	class Identidad {

		@Test
		void dosUsuarios_conElMismoId_sonElMismoUsuario() {
			Usuario usuario = Usuario.crearAdmin("admin@pasarela.local", HASH, AHORA);
			Usuario mismoUsuario = Usuario.reconstituir(
					usuario.id(), usuario.email(), usuario.hashContrasena(),
					usuario.rol(), null, usuario.creadoEn());
			Usuario otro = Usuario.crearAdmin("otro@pasarela.local", HASH, AHORA);

			assertThat(usuario).isEqualTo(mismoUsuario).hasSameHashCodeAs(mismoUsuario);
			assertThat(usuario).isNotEqualTo(otro);
			assertThat(usuario.hashCode()).isNotEqualTo(otro.hashCode());
			assertThat(usuario).isNotEqualTo("otra cosa");
		}
	}

}
