package com.pasarela.pagos;

import com.pasarela.TestcontainersConfiguration;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ComandoProcesarWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ResultadoWebhook;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LA prueba del Sprint 4 (HU-011): el MISMO evento llegando en paralelo
 * (8 hilos sincronizados con barrera) contra PostgreSQL real produce
 * EXACTAMENTE una confirmación. La constraint única es la última línea de
 * defensa y aquí se ejercita de verdad (ADR-004).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class IdempotenciaConcurrenteTest {

	private static final int HILOS = 8;

	@Autowired
	private ProcesarWebhookUseCase procesarWebhook;

	@Autowired
	private OrdenDePagoRepositorio ordenes;

	@Autowired
	private JdbcTemplate jdbc;

	@Value("${pasarela.proveedores.simulado.secreto-webhook}")
	private String secretoWebhook;

	@Test
	void elMismoEventoEnOchoHilosSimultaneos_produceUnaSolaConfirmacion() throws Exception {
		OrdenDePago orden = ordenPendienteDePago();
		String cuerpo = """
				{"idEvento": "evt-carrera-1", "tipo": "PAGO_RECIBIDO", "referencia": "%s", "monto": 40000, "pagadoEn": "%s"}
				""".formatted(orden.referencia().valor(), Instant.now()).trim();
		ComandoProcesarWebhook comando =
				new ComandoProcesarWebhook("simulado", cuerpo, firmar(cuerpo));

		CyclicBarrier barrera = new CyclicBarrier(HILOS);
		List<Callable<Object>> tareas = new ArrayList<>();
		for (int i = 0; i < HILOS; i++) {
			tareas.add(() -> {
				barrera.await(); // todos disparan EXACTAMENTE al mismo tiempo
				try {
					return procesarWebhook.procesar(comando);
				} catch (Exception excepcion) {
					return excepcion;
				}
			});
		}
		ExecutorService pool = Executors.newFixedThreadPool(HILOS);
		List<Future<Object>> resultados = pool.invokeAll(tareas);
		pool.shutdown();

		// exactamente UN hilo confirmó; el resto vio DUPLICADO; ninguno explotó
		List<Object> valores = new ArrayList<>();
		for (Future<Object> futuro : resultados) {
			valores.add(futuro.get());
		}
		assertThat(valores).noneMatch(valor -> valor instanceof Exception);
		assertThat(valores.stream().filter(ResultadoWebhook.CONFIRMADO::equals)).hasSize(1);
		assertThat(valores.stream().filter(ResultadoWebhook.DUPLICADO::equals))
				.hasSize(HILOS - 1);

		// en BD: una sola fila del evento y una sola confirmación en la orden
		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where id_externo_evento = 'evt-carrera-1'",
				Integer.class)).isEqualTo(1);
		OrdenDePago confirmada = ordenes.buscarPorId(orden.id()).orElseThrow();
		assertThat(confirmada.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
		assertThat(confirmada.historial()).hasSize(2); // CREADA→PENDIENTE, PENDIENTE→DETECTADO
	}

	private OrdenDePago ordenPendienteDePago() {
		Instant ahora = Instant.now();
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				ahora, ahora.plus(Duration.ofMinutes(15)));
		orden.registrarCobroEnProveedor(ahora);
		return ordenes.guardar(orden);
	}

	private String firmar(String cuerpo) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(
				secretoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
	}

}
