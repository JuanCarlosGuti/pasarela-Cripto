package com.pasarela.pagos;

import com.pasarela.TestcontainersConfiguration;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ReconciliarOrdenesUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulación del webhook perdido (criterio de cierre del Sprint 5): la
 * orden quedó pendiente, el webhook jamás llegó, y la reconciliación la
 * confirma sola por la misma ruta idempotente (HU-015). Reconciliar dos
 * veces no duplica nada.
 */
@SpringBootTest(properties = {
		"pasarela.proveedores.simulado.resultado-consulta=PAGADO",
		"pasarela.pagos.reconciliacion.minutos-atascada=0"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ReconciliacionTest {

	@Autowired
	private ReconciliarOrdenesUseCase reconciliarOrdenes;

	@Autowired
	private OrdenDePagoRepositorio ordenes;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void unaOrdenCuyoWebhookSePerdio_convergeSola_yReconciliarDosVecesNoDuplica() {
		Instant ahora = Instant.now();
		OrdenDePago atascada = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				ahora.minus(Duration.ofMinutes(3)), ahora.plus(Duration.ofMinutes(12)));
		atascada.registrarCobroEnProveedor(ahora.minus(Duration.ofMinutes(3)));
		ordenes.guardar(atascada);

		var primera = reconciliarOrdenes.reconciliar();
		var segunda = reconciliarOrdenes.reconciliar();

		// convergió sola: el proveedor reportó el pago y la orden se confirmó
		OrdenDePago confirmada = ordenes.buscarPorId(atascada.id()).orElseThrow();
		assertThat(confirmada.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
		assertThat(primera.confirmadas()).isGreaterThanOrEqualTo(1);

		// idempotencia de la ruta compartida: ni doble confirmación ni doble evento
		assertThat(confirmada.historial()).hasSize(2);
		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where id_externo_evento = ?",
				Integer.class, "evt-recon-" + atascada.referencia().valor())).isEqualTo(1);
		assertThat(segunda.confirmadas()).isZero();
	}

}
