package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComercioTest {

	private static final Instant AHORA = Instant.parse("2026-07-08T10:00:00Z");
	private static final Nit NIT = Nit.de("899999068-1");
	private static final CuentaLiquidacion CUENTA = new CuentaLiquidacion(
			TipoCuenta.NEQUI, "3001234567", "Tienda La Esquina SAS");

	@Nested
	class Registro {

		@Test
		void registrar_dejaElComercioPendiente_sinPoderCobrar() {
			Comercio comercio = Comercio.registrar(
					"Tienda La Esquina SAS", NIT, CUENTA, AHORA);

			assertThat(comercio.id()).isNotNull();
			assertThat(comercio.razonSocial()).isEqualTo("Tienda La Esquina SAS");
			assertThat(comercio.nit()).isEqualTo(NIT);
			assertThat(comercio.cuentaLiquidacion()).isEqualTo(CUENTA);
			assertThat(comercio.estadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
			assertThat(comercio.puedeCobrar()).isFalse();
			assertThat(comercio.registradoEn()).isEqualTo(AHORA);
		}

		@Test
		void registrar_conRazonSocialVaciaONula_lanzaExcepcion() {
			assertThatThrownBy(() -> Comercio.registrar("  ", NIT, CUENTA, AHORA))
					.isInstanceOf(ComercioInvalidoException.class)
					.hasMessageContaining("razón social");
			assertThatThrownBy(() -> Comercio.registrar(null, NIT, CUENTA, AHORA))
					.isInstanceOf(ComercioInvalidoException.class);
		}

		@Test
		void registrar_conAlgunDatoNulo_lanzaExcepcion() {
			assertThatThrownBy(() -> Comercio.registrar("Tienda", null, CUENTA, AHORA))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.registrar("Tienda", NIT, null, AHORA))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.registrar("Tienda", NIT, CUENTA, null))
					.isInstanceOf(ComercioInvalidoException.class);
		}
	}

	@Nested
	class Reconstitucion {

		@Test
		void reconstituir_restauraElEstadoTalCualVieneDePersistencia() {
			Comercio original = Comercio.registrar("Tienda", NIT, CUENTA, AHORA);

			Comercio reconstituido = Comercio.reconstituir(
					original.id(), original.razonSocial(), original.nit(),
					original.cuentaLiquidacion(), EstadoVerificacion.VERIFICADO,
					original.registradoEn());

			assertThat(reconstituido.id()).isEqualTo(original.id());
			assertThat(reconstituido.estadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADO);
			assertThat(reconstituido.puedeCobrar()).isTrue();
		}

		@Test
		void reconstituir_conCualquierDatoNulo_lanzaExcepcion() {
			Comercio c = Comercio.registrar("Tienda", NIT, CUENTA, AHORA);

			assertThatThrownBy(() -> Comercio.reconstituir(null, c.razonSocial(), c.nit(),
					c.cuentaLiquidacion(), c.estadoVerificacion(), c.registradoEn()))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), " ", c.nit(),
					c.cuentaLiquidacion(), c.estadoVerificacion(), c.registradoEn()))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), null,
					c.cuentaLiquidacion(), c.estadoVerificacion(), c.registradoEn()))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), c.nit(),
					null, c.estadoVerificacion(), c.registradoEn()))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), c.nit(),
					c.cuentaLiquidacion(), null, c.registradoEn()))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), c.nit(),
					c.cuentaLiquidacion(), c.estadoVerificacion(), null))
					.isInstanceOf(ComercioInvalidoException.class);
		}
	}

	@Nested
	class Identidad {

		@Test
		void dosComercios_conElMismoId_sonElMismoComercio() {
			Comercio comercio = Comercio.registrar("Tienda", NIT, CUENTA, AHORA);
			Comercio mismoComercio = Comercio.reconstituir(
					comercio.id(), comercio.razonSocial(), comercio.nit(),
					comercio.cuentaLiquidacion(), comercio.estadoVerificacion(),
					comercio.registradoEn());
			Comercio otro = Comercio.registrar("Otra Tienda", Nit.de("890903938-8"), CUENTA, AHORA);

			assertThat(comercio).isEqualTo(mismoComercio).hasSameHashCodeAs(mismoComercio);
			assertThat(comercio).isNotEqualTo(otro);
			assertThat(comercio.hashCode()).isNotEqualTo(otro.hashCode());
			assertThat(comercio).isNotEqualTo("otra cosa");
		}
	}

	@Nested
	class ReglaDeCobro {

		@Test
		void soloUnComercioVerificado_puedeCobrar() {
			for (EstadoVerificacion estado : EstadoVerificacion.values()) {
				Comercio comercio = comercioEnEstado(estado);

				assertThat(comercio.puedeCobrar())
						.as("puedeCobrar en %s", estado)
						.isEqualTo(estado == EstadoVerificacion.VERIFICADO);
			}
		}

		private Comercio comercioEnEstado(EstadoVerificacion estado) {
			Comercio base = Comercio.registrar("Tienda", NIT, CUENTA, AHORA);
			return Comercio.reconstituir(base.id(), base.razonSocial(), base.nit(),
					base.cuentaLiquidacion(), estado, base.registradoEn());
		}
	}

	@Nested
	class CuentaDeLiquidacion {

		@Test
		void crear_conDatosCompletos_esValida() {
			CuentaLiquidacion cuenta = new CuentaLiquidacion(
					TipoCuenta.AHORROS, "12345678901", "Tienda SAS");

			assertThat(cuenta.tipo()).isEqualTo(TipoCuenta.AHORROS);
			assertThat(cuenta.numero()).isEqualTo("12345678901");
			assertThat(cuenta.titular()).isEqualTo("Tienda SAS");
		}

		@Test
		void crear_conDatosFaltantes_lanzaExcepcion() {
			assertThatThrownBy(() -> new CuentaLiquidacion(null, "3001234567", "Tienda"))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> new CuentaLiquidacion(TipoCuenta.NEQUI, " ", "Tienda"))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", null))
					.isInstanceOf(ComercioInvalidoException.class);
		}

		@Test
		void crear_conNumeroNoNumerico_lanzaExcepcion() {
			assertThatThrownBy(() -> new CuentaLiquidacion(TipoCuenta.NEQUI, "30012ABC67", "Tienda"))
					.isInstanceOf(ComercioInvalidoException.class)
					.hasMessageContaining("número");
		}
	}

}