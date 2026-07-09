package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroCreado;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.SolicitudDeCobro;
import com.pasarela.pagos.infraestructura.salida.proveedor.ProveedorDePagoSimulado.ModoDeFallo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProveedorDePagoSimuladoTest {

	private static final SolicitudDeCobro SOLICITUD = new SolicitudDeCobro(
			new ReferenciaPago("ref-de-prueba"),
			Dinero.cop(40000),
			Instant.parse("2026-07-09T15:15:00Z"));

	@Test
	void enModoNormal_generaUnQrDeterministaConReferenciaYMonto() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.NINGUNO, 200);

		CobroCreado cobro = simulador.crearCobro(SOLICITUD);

		assertThat(cobro.contenidoQr()).isEqualTo("PAGOSIM|ref-de-prueba|40000");
		assertThat(cobro.deeplink()).isEqualTo("pasarela-sim://pagar/ref-de-prueba");
		// determinista: la misma solicitud produce exactamente el mismo QR
		assertThat(simulador.crearCobro(SOLICITUD)).isEqualTo(cobro);
	}

	@Test
	void enModoError_fallaInmediatamente_comoUn500DelProveedor() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.ERROR, 200);

		assertThatThrownBy(() -> simulador.crearCobro(SOLICITUD))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class)
				.hasMessageContaining("ERROR");
	}

	@Test
	void enModoTimeout_esperaElTiempoConfigurado_yLuegoFalla() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.TIMEOUT, 150);

		Instant antes = Instant.now();
		assertThatThrownBy(() -> simulador.crearCobro(SOLICITUD))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class)
				.hasMessageContaining("timeout");

		assertThat(Duration.between(antes, Instant.now()))
				.isGreaterThanOrEqualTo(Duration.ofMillis(150));
	}

}
