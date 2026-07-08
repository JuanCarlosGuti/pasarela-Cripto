package com.pasarela.pagos.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.excepcion.OrdenNoPuedeConfirmarseException;
import com.pasarela.pagos.dominio.excepcion.TransicionDeEstadoInvalidaException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

class OrdenDePagoTest {

	private static final Instant CREADA_EN = Instant.parse("2026-07-08T10:00:00Z");
	private static final Instant EXPIRA_EN = CREADA_EN.plus(Duration.ofMinutes(15));
	private static final Instant DENTRO_DE_LA_VENTANA = EXPIRA_EN.minusSeconds(1);
	private static final Instant DESPUES_DE_EXPIRAR = EXPIRA_EN.plusSeconds(1);

	/**
	 * Los eventos de negocio que pueden intentarse sobre una orden, para
	 * recorrer el producto cartesiano estado × evento exigido por HU-002.
	 */
	enum Accion {
		REGISTRAR_COBRO(orden -> orden.registrarCobroEnProveedor(DENTRO_DE_LA_VENTANA)),
		CONFIRMAR_PAGO(orden -> orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA)),
		EXPIRAR(orden -> orden.expirar(DESPUES_DE_EXPIRAR)),
		MARCAR_CONVERTIDA(orden -> orden.marcarComoConvertida(DENTRO_DE_LA_VENTANA)),
		MARCAR_FALLIDA(orden -> orden.marcarComoFallida("pago inválido", DENTRO_DE_LA_VENTANA)),
		ESCALAR_A_REVISION(orden -> orden.escalarARevision(DENTRO_DE_LA_VENTANA)),
		MARCAR_LIQUIDADA(orden -> orden.marcarComoLiquidada(DENTRO_DE_LA_VENTANA));

		private final Consumer<OrdenDePago> ejecucion;

		Accion(Consumer<OrdenDePago> ejecucion) {
			this.ejecucion = ejecucion;
		}

		void ejecutar(OrdenDePago orden) {
			ejecucion.accept(orden);
		}
	}

	private record TransicionValida(EstadoOrden origen, Accion accion, EstadoOrden destino) {
	}

	/** La matriz de docs/04: todo lo que no esté aquí debe rechazarse. */
	private static final List<TransicionValida> MATRIZ_DE_TRANSICIONES = List.of(
			new TransicionValida(EstadoOrden.CREADA, Accion.REGISTRAR_COBRO, EstadoOrden.PENDIENTE_PAGO),
			new TransicionValida(EstadoOrden.PENDIENTE_PAGO, Accion.CONFIRMAR_PAGO, EstadoOrden.PAGO_DETECTADO),
			new TransicionValida(EstadoOrden.PENDIENTE_PAGO, Accion.EXPIRAR, EstadoOrden.EXPIRADA),
			new TransicionValida(EstadoOrden.PAGO_DETECTADO, Accion.MARCAR_CONVERTIDA, EstadoOrden.CONVERTIDA),
			new TransicionValida(EstadoOrden.PAGO_DETECTADO, Accion.MARCAR_FALLIDA, EstadoOrden.FALLIDA),
			new TransicionValida(EstadoOrden.CONVERTIDA, Accion.MARCAR_LIQUIDADA, EstadoOrden.LIQUIDADA),
			new TransicionValida(EstadoOrden.FALLIDA, Accion.ESCALAR_A_REVISION, EstadoOrden.EN_REVISION));

	@Nested
	class Creacion {

		@Test
		void crear_dejaLaOrdenEnCreada_conHistorialVacio() {
			OrdenDePago orden = ordenNueva();

			assertThat(orden.estado()).isEqualTo(EstadoOrden.CREADA);
			assertThat(orden.historial()).isEmpty();
			assertThat(orden.id()).isNotNull();
			assertThat(orden.creadaEn()).isEqualTo(CREADA_EN);
			assertThat(orden.expiraEn()).isEqualTo(EXPIRA_EN);
		}

		@Test
		void crear_conMontoCero_lanzaExcepcion() {
			assertThatThrownBy(() -> OrdenDePago.crear(
					IdComercio.generar(), Dinero.cop(0), ReferenciaPago.generar(), CREADA_EN, EXPIRA_EN))
					.isInstanceOf(OrdenInvalidaException.class)
					.hasMessageContaining("mayor que cero");
		}

		@Test
		void crear_conExpiracionAnteriorALaCreacion_lanzaExcepcion() {
			assertThatThrownBy(() -> OrdenDePago.crear(
					IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
					CREADA_EN, CREADA_EN.minusSeconds(1)))
					.isInstanceOf(OrdenInvalidaException.class)
					.hasMessageContaining("expiración");
		}

		@Test
		void crear_conAlgunDatoNulo_lanzaExcepcion() {
			assertThatThrownBy(() -> OrdenDePago.crear(
					null, Dinero.cop(40000), ReferenciaPago.generar(), CREADA_EN, EXPIRA_EN))
					.isInstanceOf(OrdenInvalidaException.class);
			assertThatThrownBy(() -> OrdenDePago.crear(
					IdComercio.generar(), null, ReferenciaPago.generar(), CREADA_EN, EXPIRA_EN))
					.isInstanceOf(OrdenInvalidaException.class);
			assertThatThrownBy(() -> OrdenDePago.crear(
					IdComercio.generar(), Dinero.cop(40000), null, CREADA_EN, EXPIRA_EN))
					.isInstanceOf(OrdenInvalidaException.class);
			assertThatThrownBy(() -> OrdenDePago.crear(
					IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(), null, EXPIRA_EN))
					.isInstanceOf(OrdenInvalidaException.class);
			assertThatThrownBy(() -> OrdenDePago.crear(
					IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(), CREADA_EN, null))
					.isInstanceOf(OrdenInvalidaException.class);
		}
	}

	@Nested
	class MatrizDeTransiciones {

		@ParameterizedTest(name = "{0} + {1} → {2}")
		@MethodSource("transicionesValidas")
		void transicionValida_cambiaDeEstadoYRegistraLaTransicion(
				EstadoOrden origen, Accion accion, EstadoOrden destino) {
			OrdenDePago orden = ordenEnEstado(origen);
			int transicionesPrevias = orden.historial().size();

			accion.ejecutar(orden);

			assertThat(orden.estado()).isEqualTo(destino);
			assertThat(orden.historial()).hasSize(transicionesPrevias + 1);
			TransicionEstado ultima = orden.historial().getLast();
			assertThat(ultima.desde()).isEqualTo(origen);
			assertThat(ultima.hacia()).isEqualTo(destino);
			assertThat(ultima.momento()).isNotNull();
		}

		@ParameterizedTest(name = "{0} + {1} se rechaza")
		@MethodSource("transicionesInvalidas")
		void transicionInvalida_lanzaExcepcionYNoCambiaNada(EstadoOrden origen, Accion accion) {
			OrdenDePago orden = ordenEnEstado(origen);
			List<TransicionEstado> historialPrevio = orden.historial();

			assertThatThrownBy(() -> accion.ejecutar(orden))
					.isInstanceOf(TransicionDeEstadoInvalidaException.class);

			assertThat(orden.estado()).isEqualTo(origen);
			assertThat(orden.historial()).isEqualTo(historialPrevio);
		}

		static Stream<Arguments> transicionesValidas() {
			return MATRIZ_DE_TRANSICIONES.stream()
					.map(transicion -> Arguments.of(
							transicion.origen(), transicion.accion(), transicion.destino()));
		}

		static Stream<Arguments> transicionesInvalidas() {
			return Arrays.stream(EstadoOrden.values())
					.flatMap(estado -> Arrays.stream(Accion.values())
							.filter(accion -> !esTransicionValida(estado, accion))
							.map(accion -> Arguments.of(estado, accion)));
		}

		private static boolean esTransicionValida(EstadoOrden origen, Accion accion) {
			return MATRIZ_DE_TRANSICIONES.stream()
					.anyMatch(transicion ->
							transicion.origen() == origen && transicion.accion() == accion);
		}
	}

	@Nested
	class ConfirmacionDePago {

		@Test
		void confirmarPago_pendienteYDentroDeLaVentana_guardaTimestampDeLaTransicion() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PENDIENTE_PAGO);

			orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA);

			assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
			assertThat(orden.historial().getLast().momento()).isEqualTo(DENTRO_DE_LA_VENTANA);
		}

		@Test
		void confirmarPago_conLaOrdenExpirada_lanzaExcepcionYNoCambiaNada() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PENDIENTE_PAGO);

			assertThatThrownBy(() -> orden.confirmarPago(eventoDePagoDe(orden), DESPUES_DE_EXPIRAR))
					.isInstanceOf(OrdenNoPuedeConfirmarseException.class)
					.hasMessageContaining("expir");

			assertThat(orden.estado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
		}

		@Test
		void confirmarPago_enEstadoNoConfirmable_lanzaLaExcepcionEspecifica() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.EXPIRADA);

			assertThatThrownBy(() -> orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA))
					.isInstanceOf(OrdenNoPuedeConfirmarseException.class);
		}

		@Test
		void confirmarPago_conEventoNulo_lanzaExcepcion() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PENDIENTE_PAGO);

			assertThatThrownBy(() -> orden.confirmarPago(null, DENTRO_DE_LA_VENTANA))
					.isInstanceOf(OrdenInvalidaException.class);
		}

		@Test
		void puedeConfirmarse_soloEnPendientePago() {
			for (EstadoOrden estado : EstadoOrden.values()) {
				assertThat(ordenEnEstado(estado).puedeConfirmarse())
						.as("puedeConfirmarse en %s", estado)
						.isEqualTo(estado == EstadoOrden.PENDIENTE_PAGO);
			}
		}
	}

	@Nested
	class Expiracion {

		@Test
		void expirar_antesDelVencimiento_lanzaExcepcionYNoCambiaNada() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PENDIENTE_PAGO);

			assertThatThrownBy(() -> orden.expirar(DENTRO_DE_LA_VENTANA))
					.isInstanceOf(TransicionDeEstadoInvalidaException.class);

			assertThat(orden.estado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
		}

		@Test
		void estaExpirada_exactamenteEnElLimite_esFalse() {
			assertThat(ordenNueva().estaExpirada(EXPIRA_EN)).isFalse();
		}

		@Test
		void estaExpirada_unSegundoDespuesDelLimite_esTrue() {
			assertThat(ordenNueva().estaExpirada(DESPUES_DE_EXPIRAR)).isTrue();
		}
	}

	@Nested
	class Fallo {

		@Test
		void marcarComoFallida_sinMotivo_lanzaExcepcionYNoCambiaNada() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PAGO_DETECTADO);

			assertThatThrownBy(() -> orden.marcarComoFallida(null, DENTRO_DE_LA_VENTANA))
					.isInstanceOf(OrdenInvalidaException.class);
			assertThatThrownBy(() -> orden.marcarComoFallida("  ", DENTRO_DE_LA_VENTANA))
					.isInstanceOf(OrdenInvalidaException.class);

			assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
		}

		@Test
		void marcarComoFallida_guardaElMotivoEnElHistorial() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PAGO_DETECTADO);

			orden.marcarComoFallida("monto distinto al esperado", DENTRO_DE_LA_VENTANA);

			assertThat(orden.historial().getLast().motivo())
					.isEqualTo("monto distinto al esperado");
		}
	}

	@Nested
	class Historial {

		@Test
		void elCaminoFelizCompleto_quedaRegistradoConTimestamps() {
			OrdenDePago orden = ordenNueva();
			Instant momento1 = CREADA_EN.plusSeconds(10);
			Instant momento2 = CREADA_EN.plusSeconds(60);
			Instant momento3 = CREADA_EN.plusSeconds(120);
			Instant momento4 = CREADA_EN.plusSeconds(180);

			orden.registrarCobroEnProveedor(momento1);
			orden.confirmarPago(eventoDePagoDe(orden), momento2);
			orden.marcarComoConvertida(momento3);
			orden.marcarComoLiquidada(momento4);

			assertThat(orden.estado()).isEqualTo(EstadoOrden.LIQUIDADA);
			assertThat(orden.historial()).extracting(
							TransicionEstado::desde, TransicionEstado::hacia, TransicionEstado::momento)
					.containsExactly(
							tuple(EstadoOrden.CREADA, EstadoOrden.PENDIENTE_PAGO, momento1),
							tuple(EstadoOrden.PENDIENTE_PAGO, EstadoOrden.PAGO_DETECTADO, momento2),
							tuple(EstadoOrden.PAGO_DETECTADO, EstadoOrden.CONVERTIDA, momento3),
							tuple(EstadoOrden.CONVERTIDA, EstadoOrden.LIQUIDADA, momento4));
		}

		@Test
		void elHistorialExpuesto_noPuedeModificarseDesdeFuera() {
			OrdenDePago orden = ordenEnEstado(EstadoOrden.PENDIENTE_PAGO);
			List<TransicionEstado> historial = orden.historial();

			assertThatThrownBy(() -> historial.add(
					TransicionEstado.de(EstadoOrden.CREADA, EstadoOrden.PENDIENTE_PAGO, CREADA_EN)))
					.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@Nested
	class Identidad {

		@Test
		void dosOrdenes_conElMismoId_sonLaMismaOrden() {
			OrdenDePago orden = ordenNueva();
			OrdenDePago otra = ordenNueva();

			assertThat(orden).isEqualTo(orden);
			assertThat(orden).isNotEqualTo(otra);
		}
	}

	// --- ayudas de construcción: llegan a cada estado SOLO por transiciones válidas ---

	private static OrdenDePago ordenNueva() {
		return OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				CREADA_EN, EXPIRA_EN);
	}

	private static EventoPago eventoDePagoDe(OrdenDePago orden) {
		return new EventoPago(orden.referencia(), orden.monto(), DENTRO_DE_LA_VENTANA);
	}

	private static OrdenDePago ordenEnEstado(EstadoOrden destino) {
		OrdenDePago orden = ordenNueva();
		switch (destino) {
			case CREADA -> { }
			case PENDIENTE_PAGO -> orden.registrarCobroEnProveedor(CREADA_EN);
			case PAGO_DETECTADO -> {
				orden.registrarCobroEnProveedor(CREADA_EN);
				orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA);
			}
			case CONVERTIDA -> {
				orden.registrarCobroEnProveedor(CREADA_EN);
				orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA);
				orden.marcarComoConvertida(DENTRO_DE_LA_VENTANA);
			}
			case LIQUIDADA -> {
				orden.registrarCobroEnProveedor(CREADA_EN);
				orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA);
				orden.marcarComoConvertida(DENTRO_DE_LA_VENTANA);
				orden.marcarComoLiquidada(DENTRO_DE_LA_VENTANA);
			}
			case EXPIRADA -> {
				orden.registrarCobroEnProveedor(CREADA_EN);
				orden.expirar(DESPUES_DE_EXPIRAR);
			}
			case FALLIDA -> {
				orden.registrarCobroEnProveedor(CREADA_EN);
				orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA);
				orden.marcarComoFallida("pago inválido", DENTRO_DE_LA_VENTANA);
			}
			case EN_REVISION -> {
				orden.registrarCobroEnProveedor(CREADA_EN);
				orden.confirmarPago(eventoDePagoDe(orden), DENTRO_DE_LA_VENTANA);
				orden.marcarComoFallida("pago inválido", DENTRO_DE_LA_VENTANA);
				orden.escalarARevision(DENTRO_DE_LA_VENTANA);
			}
		}
		assertThat(orden.estado()).isEqualTo(destino);
		return orden;
	}

}