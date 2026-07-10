package com.pasarela.pagos;

import com.pasarela.TestcontainersConfiguration;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.EventoPago;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ExpirarOrdenesVencidasUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La carrera expiración-vs-pago (HU-014) contra PostgreSQL real: dos hilos
 * cargan la MISMA orden pendiente; uno la expira y el otro la confirma, al
 * mismo tiempo. Solo una transición gana; la otra recibe el fallo optimista
 * y se rechaza limpiamente — la orden jamás queda corrupta.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CarreraExpiracionVsPagoTest {

	@Autowired
	private OrdenDePagoRepositorio ordenes;

	@Autowired
	private ExpirarOrdenesVencidasUseCase expirarOrdenes;

	@Test
	void soloUnaTransicionGana_yLaOrdenQuedaConsistente() throws Exception {
		Instant ahora = Instant.now();
		// pendiente cuya ventana vence "ahora mismo": el escenario de la carrera
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				ahora.minus(Duration.ofMinutes(15)), ahora);
		orden.registrarCobroEnProveedor(ahora.minus(Duration.ofMinutes(15)));
		ordenes.guardar(orden);

		// ambos hilos cargan la MISMA versión antes de disparar
		OrdenDePago cargadaPorElJob = ordenes.buscarPorId(orden.id()).orElseThrow();
		OrdenDePago cargadaPorElWebhook = ordenes.buscarPorId(orden.id()).orElseThrow();
		CyclicBarrier barrera = new CyclicBarrier(2);

		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future<Object> job = pool.submit(() -> {
			barrera.await();
			try {
				cargadaPorElJob.expirar(ahora.plusSeconds(1));
				ordenes.guardar(cargadaPorElJob);
				return "EXPIRO";
			} catch (OptimisticLockingFailureException perdio) {
				return perdio;
			}
		});
		Future<Object> webhook = pool.submit(() -> {
			barrera.await();
			try {
				cargadaPorElWebhook.confirmarPago(new EventoPago(
						orden.referencia(), Dinero.cop(40000), ahora), ahora.minusSeconds(1));
				ordenes.guardar(cargadaPorElWebhook);
				return "CONFIRMO";
			} catch (OptimisticLockingFailureException perdio) {
				return perdio;
			}
		});
		Object resultadoJob = job.get();
		Object resultadoWebhook = webhook.get();
		pool.shutdown();

		// exactamente un ganador y un perdedor con fallo optimista limpio
		List<Object> resultados = List.of(resultadoJob, resultadoWebhook);
		assertThat(resultados).filteredOn(String.class::isInstance).hasSize(1);
		assertThat(resultados)
				.filteredOn(OptimisticLockingFailureException.class::isInstance).hasSize(1);

		// el estado final es EL DEL GANADOR, con su única transición registrada
		OrdenDePago finalEnBd = ordenes.buscarPorId(orden.id()).orElseThrow();
		assertThat(finalEnBd.estado()).isIn(EstadoOrden.EXPIRADA, EstadoOrden.PAGO_DETECTADO);
		assertThat(finalEnBd.historial()).hasSize(2);
		String ganador = (String) resultados.stream()
				.filter(String.class::isInstance).findFirst().orElseThrow();
		assertThat(finalEnBd.estado()).isEqualTo(
				"EXPIRO".equals(ganador) ? EstadoOrden.EXPIRADA : EstadoOrden.PAGO_DETECTADO);
	}

	@Test
	void elJobExpiraLasVencidas_respetaLasVigentes_yEsIdempotente() {
		Instant ahora = Instant.now();
		OrdenDePago vencida = pendiente(ahora.minus(Duration.ofMinutes(30)),
				ahora.minus(Duration.ofMinutes(15)));
		OrdenDePago vigente = pendiente(ahora, ahora.plus(Duration.ofMinutes(15)));

		var primera = expirarOrdenes.expirarVencidas();
		var segunda = expirarOrdenes.expirarVencidas();

		assertThat(ordenes.buscarPorId(vencida.id()).orElseThrow().estado())
				.isEqualTo(EstadoOrden.EXPIRADA);
		assertThat(ordenes.buscarPorId(vigente.id()).orElseThrow().estado())
				.isEqualTo(EstadoOrden.PENDIENTE_PAGO);
		assertThat(primera.expiradas()).isGreaterThanOrEqualTo(1);
		// idempotente: la segunda corrida no encuentra nada nuevo de esta tanda
		assertThat(ordenes.buscarPorId(vencida.id()).orElseThrow().historial()).hasSize(2);
		assertThat(segunda.carrerasDetectadas()).isZero();
	}

	private OrdenDePago pendiente(Instant creadaEn, Instant expiraEn) {
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				creadaEn, expiraEn);
		orden.registrarCobroEnProveedor(creadaEn);
		return ordenes.guardar(orden);
	}

}
