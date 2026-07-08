package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.TestcontainersConfiguration;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.EventoPago;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.modelo.TransicionEstado;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integración del adaptador JPA contra PostgreSQL real (Testcontainers):
 * ida y vuelta dominio↔BD sin pérdidas, búsquedas con falsos positivos y la
 * restricción de unicidad de la referencia verificada a nivel SQL (T-005).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OrdenJpaMapper.class, OrdenDePagoRepositorioJpa.class})
class OrdenDePagoRepositorioJpaTest {

	private static final Instant CREADA_EN = Instant.parse("2026-07-08T10:00:00Z");
	private static final Instant EXPIRA_EN = CREADA_EN.plus(Duration.ofMinutes(15));
	private static final Instant DENTRO_DE_LA_VENTANA = EXPIRA_EN.minusSeconds(1);
	private static final Instant DESPUES_DE_EXPIRAR = EXPIRA_EN.plusSeconds(1);

	@Autowired
	private OrdenDePagoRepositorio repositorio;

	@Nested
	class IdaYVuelta {

		@Test
		void unaOrdenConHistorialCompleto_sobreviveElViajeSinPerdidas() {
			OrdenDePago original = ordenNueva();
			original.registrarCobroEnProveedor(CREADA_EN.plusSeconds(10));
			original.confirmarPago(eventoDePagoDe(original), CREADA_EN.plusSeconds(60));
			original.marcarComoFallida("monto distinto al esperado", CREADA_EN.plusSeconds(90));

			repositorio.guardar(original);
			OrdenDePago recuperada = repositorio.buscarPorId(original.id()).orElseThrow();

			assertThat(recuperada.id()).isEqualTo(original.id());
			assertThat(recuperada.comercioId()).isEqualTo(original.comercioId());
			assertThat(recuperada.monto()).isEqualTo(original.monto());
			assertThat(recuperada.referencia()).isEqualTo(original.referencia());
			assertThat(recuperada.estado()).isEqualTo(EstadoOrden.FALLIDA);
			assertThat(recuperada.creadaEn()).isEqualTo(original.creadaEn());
			assertThat(recuperada.expiraEn()).isEqualTo(original.expiraEn());
			// el historial completo: orden, timestamps y motivo intactos
			assertThat(recuperada.historial()).isEqualTo(original.historial());
			assertThat(recuperada.historial().getLast().motivo())
					.isEqualTo("monto distinto al esperado");
		}

		@Test
		void guardarTrasUnaNuevaTransicion_actualizaSinDuplicarLaOrden() {
			OrdenDePago orden = ordenNueva();
			orden.registrarCobroEnProveedor(CREADA_EN.plusSeconds(10));
			repositorio.guardar(orden);

			OrdenDePago cargada = repositorio.buscarPorId(orden.id()).orElseThrow();
			cargada.confirmarPago(eventoDePagoDe(cargada), DENTRO_DE_LA_VENTANA);
			repositorio.guardar(cargada);

			OrdenDePago recargada = repositorio.buscarPorId(orden.id()).orElseThrow();
			assertThat(recargada.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
			assertThat(recargada.historial()).hasSize(2);
		}

		@Test
		void unaOrdenReconstituida_puedeSeguirTransicionando() {
			OrdenDePago orden = ordenNueva();
			orden.registrarCobroEnProveedor(CREADA_EN.plusSeconds(10));
			repositorio.guardar(orden);

			OrdenDePago cargada = repositorio.buscarPorId(orden.id()).orElseThrow();
			cargada.confirmarPago(eventoDePagoDe(cargada), DENTRO_DE_LA_VENTANA);
			cargada.marcarComoConvertida(DENTRO_DE_LA_VENTANA);
			cargada.marcarComoLiquidada(DENTRO_DE_LA_VENTANA);
			repositorio.guardar(cargada);

			assertThat(repositorio.buscarPorId(orden.id()).orElseThrow().estado())
					.isEqualTo(EstadoOrden.LIQUIDADA);
		}
	}

	@Nested
	class BusquedaPorReferencia {

		@Test
		void buscarPorReferencia_devuelveLaOrdenCorrecta_noOtra() {
			OrdenDePago buscada = ordenNueva();
			OrdenDePago otra = ordenNueva(); // falso positivo potencial
			repositorio.guardar(buscada);
			repositorio.guardar(otra);

			Optional<OrdenDePago> resultado = repositorio.buscarPorReferencia(buscada.referencia());

			assertThat(resultado).isPresent();
			assertThat(resultado.get().id()).isEqualTo(buscada.id());
		}

		@Test
		void buscarPorReferencia_inexistente_devuelveVacio() {
			repositorio.guardar(ordenNueva());

			assertThat(repositorio.buscarPorReferencia(ReferenciaPago.generar())).isEmpty();
		}
	}

	@Nested
	class BusquedaDePendientesExpiradas {

		@Test
		void devuelveSoloLasPendientesVencidas_yNingunFalsoPositivo() {
			OrdenDePago pendienteVencida = ordenNueva();
			pendienteVencida.registrarCobroEnProveedor(CREADA_EN);

			OrdenDePago pendienteVigente = ordenConVentana(Duration.ofHours(2));
			pendienteVigente.registrarCobroEnProveedor(CREADA_EN);

			OrdenDePago creadaVencida = ordenNueva(); // CREADA: nunca se registró el cobro

			OrdenDePago yaExpirada = ordenNueva();
			yaExpirada.registrarCobroEnProveedor(CREADA_EN);
			yaExpirada.expirar(DESPUES_DE_EXPIRAR);

			OrdenDePago pagadaVencida = ordenNueva();
			pagadaVencida.registrarCobroEnProveedor(CREADA_EN);
			pagadaVencida.confirmarPago(eventoDePagoDe(pagadaVencida), DENTRO_DE_LA_VENTANA);

			repositorio.guardar(pendienteVencida);
			repositorio.guardar(pendienteVigente);
			repositorio.guardar(creadaVencida);
			repositorio.guardar(yaExpirada);
			repositorio.guardar(pagadaVencida);

			List<OrdenDePago> expiradas = repositorio.buscarPendientesExpiradas(DESPUES_DE_EXPIRAR);

			assertThat(expiradas).extracting(OrdenDePago::id)
					.containsExactly(pendienteVencida.id());
		}

		@Test
		void enElLimiteExacto_laOrdenTodaviaNoEstaVencida() {
			// Mismo criterio que OrdenDePago.estaExpirada: vencida solo DESPUÉS del límite
			OrdenDePago pendiente = ordenNueva();
			pendiente.registrarCobroEnProveedor(CREADA_EN);
			repositorio.guardar(pendiente);

			assertThat(repositorio.buscarPendientesExpiradas(EXPIRA_EN)).isEmpty();
			assertThat(repositorio.buscarPendientesExpiradas(DESPUES_DE_EXPIRAR)).hasSize(1);
		}
	}

	@Nested
	class UnicidadDeLaReferencia {

		@Test
		void dosOrdenesConLaMismaReferencia_fallanPorConstraintDeBaseDeDatos() {
			ReferenciaPago referenciaRepetida = ReferenciaPago.generar();
			repositorio.guardar(ordenConReferencia(referenciaRepetida));

			assertThatThrownBy(() -> repositorio.guardar(ordenConReferencia(referenciaRepetida)))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	// --- ayudas ---

	private static OrdenDePago ordenNueva() {
		return ordenConReferencia(ReferenciaPago.generar());
	}

	private static OrdenDePago ordenConReferencia(ReferenciaPago referencia) {
		return OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), referencia, CREADA_EN, EXPIRA_EN);
	}

	private static OrdenDePago ordenConVentana(Duration ventana) {
		return OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				CREADA_EN, CREADA_EN.plus(ventana));
	}

	private static EventoPago eventoDePagoDe(OrdenDePago orden) {
		return new EventoPago(orden.referencia(), orden.monto(), DENTRO_DE_LA_VENTANA);
	}

}
