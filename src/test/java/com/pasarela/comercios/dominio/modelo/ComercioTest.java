package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.VerificacionInvalidaException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComercioTest {

	private static final Instant AHORA = Instant.parse("2026-07-08T10:00:00Z");
	private static final Instant DESPUES = AHORA.plusSeconds(3600);
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
			assertThat(comercio.motivoDecision()).isNull();
			assertThat(comercio.decisionEn()).isNull();
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
	class Verificacion {

		/** Las decisiones del Admin sobre un comercio (HU-005). */
		enum Accion {
			VERIFICAR(comercio -> comercio.verificar(DESPUES)),
			RECHAZAR(comercio -> comercio.rechazar("documentos inconsistentes", DESPUES)),
			SUSPENDER(comercio -> comercio.suspender("actividad inusual", DESPUES)),
			REACTIVAR(comercio -> comercio.reactivar(DESPUES));

			private final Consumer<Comercio> ejecucion;

			Accion(Consumer<Comercio> ejecucion) {
				this.ejecucion = ejecucion;
			}

			void ejecutar(Comercio comercio) {
				ejecucion.accept(comercio);
			}
		}

		private record TransicionValida(
				EstadoVerificacion origen, Accion accion, EstadoVerificacion destino) {
		}

		private static final List<TransicionValida> MATRIZ = List.of(
				new TransicionValida(EstadoVerificacion.PENDIENTE, Accion.VERIFICAR,
						EstadoVerificacion.VERIFICADO),
				new TransicionValida(EstadoVerificacion.PENDIENTE, Accion.RECHAZAR,
						EstadoVerificacion.RECHAZADO),
				new TransicionValida(EstadoVerificacion.VERIFICADO, Accion.SUSPENDER,
						EstadoVerificacion.SUSPENDIDO),
				new TransicionValida(EstadoVerificacion.SUSPENDIDO, Accion.REACTIVAR,
						EstadoVerificacion.VERIFICADO));

		@ParameterizedTest(name = "{0} + {1} → {2}")
		@MethodSource("transicionesValidas")
		void decisionValida_cambiaElEstadoYGuardaElMomento(
				EstadoVerificacion origen, Accion accion, EstadoVerificacion destino) {
			Comercio comercio = comercioEnEstado(origen);

			accion.ejecutar(comercio);

			assertThat(comercio.estadoVerificacion()).isEqualTo(destino);
			assertThat(comercio.decisionEn()).isEqualTo(DESPUES);
		}

		@ParameterizedTest(name = "{0} + {1} se rechaza")
		@MethodSource("transicionesInvalidas")
		void decisionInvalida_lanzaExcepcionYNoCambiaNada(
				EstadoVerificacion origen, Accion accion) {
			Comercio comercio = comercioEnEstado(origen);

			assertThatThrownBy(() -> accion.ejecutar(comercio))
					.isInstanceOf(VerificacionInvalidaException.class);

			assertThat(comercio.estadoVerificacion()).isEqualTo(origen);
		}

		static Stream<Arguments> transicionesValidas() {
			return MATRIZ.stream().map(transicion ->
					Arguments.of(transicion.origen(), transicion.accion(), transicion.destino()));
		}

		static Stream<Arguments> transicionesInvalidas() {
			return Arrays.stream(EstadoVerificacion.values())
					.flatMap(estado -> Arrays.stream(Accion.values())
							.filter(accion -> MATRIZ.stream().noneMatch(transicion ->
									transicion.origen() == estado && transicion.accion() == accion))
							.map(accion -> Arguments.of(estado, accion)));
		}

		@Test
		void verificarUnSuspendido_seRechaza_soloReactivacionExplicita() {
			// Criterio literal de HU-005
			Comercio suspendido = comercioEnEstado(EstadoVerificacion.SUSPENDIDO);

			assertThatThrownBy(() -> suspendido.verificar(DESPUES))
					.isInstanceOf(VerificacionInvalidaException.class);

			suspendido.reactivar(DESPUES);
			assertThat(suspendido.puedeCobrar()).isTrue();
		}

		@Test
		void rechazarYSuspender_exigenMotivo() {
			Comercio pendiente = comercioEnEstado(EstadoVerificacion.PENDIENTE);
			Comercio verificado = comercioEnEstado(EstadoVerificacion.VERIFICADO);

			assertThatThrownBy(() -> pendiente.rechazar(null, DESPUES))
					.isInstanceOf(ComercioInvalidoException.class)
					.hasMessageContaining("motivo");
			assertThatThrownBy(() -> pendiente.rechazar("  ", DESPUES))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> verificado.suspender(null, DESPUES))
					.isInstanceOf(ComercioInvalidoException.class);

			assertThat(pendiente.estadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
			assertThat(verificado.estadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADO);
		}

		@Test
		void elMotivoDeLaDecision_quedaGuardado() {
			Comercio comercio = comercioEnEstado(EstadoVerificacion.PENDIENTE);

			comercio.rechazar("documentos inconsistentes", DESPUES);

			assertThat(comercio.motivoDecision()).isEqualTo("documentos inconsistentes");
			assertThat(comercio.decisionEn()).isEqualTo(DESPUES);
		}

		@Test
		void verificar_limpiaElMotivoDeDecisionesAnteriores() {
			Comercio comercio = comercioEnEstado(EstadoVerificacion.SUSPENDIDO);
			assertThat(comercio.motivoDecision()).isNotNull();

			comercio.reactivar(DESPUES);

			assertThat(comercio.motivoDecision()).isNull();
		}

		@Test
		void decidir_conMomentoNulo_lanzaExcepcionYNoCambiaNada() {
			Comercio comercio = comercioEnEstado(EstadoVerificacion.PENDIENTE);

			assertThatThrownBy(() -> comercio.verificar(null))
					.isInstanceOf(ComercioInvalidoException.class);

			assertThat(comercio.estadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
		}
	}

	@Nested
	class ReglaDeCobro {

		@Test
		void soloUnComercioVerificado_puedeCobrar() {
			for (EstadoVerificacion estado : EstadoVerificacion.values()) {
				assertThat(comercioEnEstado(estado).puedeCobrar())
						.as("puedeCobrar en %s", estado)
						.isEqualTo(estado == EstadoVerificacion.VERIFICADO);
			}
		}
	}

	@Nested
	class Reconstitucion {

		@Test
		void reconstituir_restauraElEstadoYLaUltimaDecisionTalCual() {
			Comercio original = comercioEnEstado(EstadoVerificacion.SUSPENDIDO);

			Comercio reconstituido = reconstituirCopia(original);

			assertThat(reconstituido.id()).isEqualTo(original.id());
			assertThat(reconstituido.estadoVerificacion()).isEqualTo(EstadoVerificacion.SUSPENDIDO);
			assertThat(reconstituido.motivoDecision()).isEqualTo("actividad inusual");
			assertThat(reconstituido.decisionEn()).isEqualTo(DESPUES);
			assertThat(reconstituido.puedeCobrar()).isFalse();
		}

		@Test
		void unComercioReconstituido_sigueRespetandoLasTransiciones() {
			Comercio reconstituido = reconstituirCopia(
					comercioEnEstado(EstadoVerificacion.SUSPENDIDO));

			assertThatThrownBy(() -> reconstituido.verificar(DESPUES))
					.isInstanceOf(VerificacionInvalidaException.class);

			reconstituido.reactivar(DESPUES);
			assertThat(reconstituido.puedeCobrar()).isTrue();
		}

		@Test
		void reconstituir_conCualquierDatoObligatorioNulo_lanzaExcepcion() {
			Comercio c = comercioEnEstado(EstadoVerificacion.PENDIENTE);

			assertThatThrownBy(() -> Comercio.reconstituir(null, c.razonSocial(), c.nit(),
					c.cuentaLiquidacion(), c.estadoVerificacion(), c.registradoEn(), null, null))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), " ", c.nit(),
					c.cuentaLiquidacion(), c.estadoVerificacion(), c.registradoEn(), null, null))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), null,
					c.cuentaLiquidacion(), c.estadoVerificacion(), c.registradoEn(), null, null))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), c.nit(),
					null, c.estadoVerificacion(), c.registradoEn(), null, null))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), c.nit(),
					c.cuentaLiquidacion(), null, c.registradoEn(), null, null))
					.isInstanceOf(ComercioInvalidoException.class);
			assertThatThrownBy(() -> Comercio.reconstituir(c.id(), c.razonSocial(), c.nit(),
					c.cuentaLiquidacion(), c.estadoVerificacion(), null, null, null))
					.isInstanceOf(ComercioInvalidoException.class);
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

	@Nested
	class Identidad {

		@Test
		void dosComercios_conElMismoId_sonElMismoComercio() {
			Comercio comercio = Comercio.registrar("Tienda", NIT, CUENTA, AHORA);
			Comercio mismoComercio = reconstituirCopia(comercio);
			Comercio otro = Comercio.registrar("Otra Tienda", Nit.de("890903938-8"), CUENTA, AHORA);

			assertThat(comercio).isEqualTo(mismoComercio).hasSameHashCodeAs(mismoComercio);
			assertThat(comercio).isNotEqualTo(otro);
			assertThat(comercio.hashCode()).isNotEqualTo(otro.hashCode());
			assertThat(comercio).isNotEqualTo("otra cosa");
		}
	}

	// --- ayudas: cada estado se alcanza SOLO por decisiones válidas ---

	private static Comercio comercioEnEstado(EstadoVerificacion destino) {
		Comercio comercio = Comercio.registrar("Tienda", NIT, CUENTA, AHORA);
		switch (destino) {
			case PENDIENTE -> { }
			case VERIFICADO -> comercio.verificar(DESPUES);
			case RECHAZADO -> comercio.rechazar("documentos inconsistentes", DESPUES);
			case SUSPENDIDO -> {
				comercio.verificar(DESPUES);
				comercio.suspender("actividad inusual", DESPUES);
			}
		}
		assertThat(comercio.estadoVerificacion()).isEqualTo(destino);
		return comercio;
	}

	private static Comercio reconstituirCopia(Comercio original) {
		return Comercio.reconstituir(
				original.id(), original.razonSocial(), original.nit(),
				original.cuentaLiquidacion(), original.estadoVerificacion(),
				original.registradoEn(), original.motivoDecision(), original.decisionEn());
	}

}