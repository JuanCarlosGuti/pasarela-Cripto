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

	private static final String SECRETO = "secreto-webhook-de-prueba";
	private static final SolicitudDeCobro SOLICITUD = new SolicitudDeCobro(
			new ReferenciaPago("ref-de-prueba"),
			Dinero.cop(40000),
			Instant.parse("2026-07-09T15:15:00Z"));

	@Test
	void enModoNormal_generaUnQrDeterministaConReferenciaYMonto() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.NINGUNO, 200, SECRETO);

		CobroCreado cobro = simulador.crearCobro(SOLICITUD);

		assertThat(cobro.contenidoQr()).isEqualTo("PAGOSIM|ref-de-prueba|40000");
		assertThat(cobro.deeplink()).isEqualTo("pasarela-sim://pagar/ref-de-prueba");
		// determinista: la misma solicitud produce exactamente el mismo QR
		assertThat(simulador.crearCobro(SOLICITUD)).isEqualTo(cobro);
	}

	@Test
	void enModoError_fallaInmediatamente_comoUn500DelProveedor() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.ERROR, 200, SECRETO);

		assertThatThrownBy(() -> simulador.crearCobro(SOLICITUD))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class)
				.hasMessageContaining("ERROR");
	}

	@Test
	void laFirma_esUnHmacSha256DelCuerpoConElSecretoCompartido() throws Exception {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.NINGUNO, 200, SECRETO);
		String cuerpo = "{\"idEvento\":\"evt-1\"}";
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
		mac.init(new javax.crypto.spec.SecretKeySpec(
				SECRETO.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
		String firmaCorrecta = java.util.HexFormat.of()
				.formatHex(mac.doFinal(cuerpo.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		assertThat(simulador.firmaValida(cuerpo, firmaCorrecta)).isTrue();
		assertThat(simulador.firmaValida(cuerpo, "falsificada")).isFalse();
		assertThat(simulador.firmaValida(cuerpo, null)).isFalse();
		assertThat(simulador.firmaValida(cuerpo, " ")).isFalse();
	}

	@Test
	void interpretarWebhook_traduceElPayloadAlLenguajeDelDominio() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.NINGUNO, 200, SECRETO);
		String cuerpo = """
				{"idEvento": "evt-9", "tipo": "PAGO_RECIBIDO", "referencia": "ref-9", "monto": 40000, "pagadoEn": "2026-07-09T15:05:00Z"}
				""";

		var webhook = simulador.interpretarWebhook(cuerpo);

		assertThat(webhook.idExternoEvento()).isEqualTo("evt-9");
		assertThat(webhook.tipo()).isEqualTo("PAGO_RECIBIDO");
		assertThat(webhook.referencia()).isEqualTo(new ReferenciaPago("ref-9"));
		assertThat(webhook.monto()).isEqualTo(Dinero.cop(40000));
		assertThat(webhook.pagadoEn()).isEqualTo(Instant.parse("2026-07-09T15:05:00Z"));
	}

	@Test
	void interpretarWebhook_conPayloadMalformadoOIncompleto_lanzaExcepcion() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.NINGUNO, 200, SECRETO);

		assertThatThrownBy(() -> simulador.interpretarWebhook("esto no es json"))
				.isInstanceOf(com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException.class);
		assertThatThrownBy(() -> simulador.interpretarWebhook("{\"tipo\": \"PAGO_RECIBIDO\"}"))
				.isInstanceOf(com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException.class)
				.hasMessageContaining("idEvento");
	}

	@Test
	void enModoTimeout_esperaElTiempoConfigurado_yLuegoFalla() {
		ProveedorDePagoSimulado simulador = new ProveedorDePagoSimulado(ModoDeFallo.TIMEOUT, 150, SECRETO);

		Instant antes = Instant.now();
		assertThatThrownBy(() -> simulador.crearCobro(SOLICITUD))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class)
				.hasMessageContaining("timeout");

		assertThat(Duration.between(antes, Instant.now()))
				.isGreaterThanOrEqualTo(Duration.ofMillis(150));
	}

}
