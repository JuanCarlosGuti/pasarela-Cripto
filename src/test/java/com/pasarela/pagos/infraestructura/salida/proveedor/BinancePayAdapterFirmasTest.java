package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.WebhookDelProveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verificación de firmas del webhook de Binance Pay (HU-021) e
 * interpretación del payload. La firma real es RSA (SHA256withRSA) sobre
 * {@code timestamp\nnonce\ncuerpo\n}, compuesta por el controller como
 * {@code timestamp|nonce|firmaBase64}; la de reconciliación es HMAC interna
 * con prefijo {@code recon:}.
 */
class BinancePayAdapterFirmasTest {

	private static final String SECRETO = "secreto-hmac-de-prueba";

	private KeyPair claves;
	private BinancePayAdapter adaptador;

	@BeforeEach
	void crearAdaptador() throws Exception {
		KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		claves = generador.generateKeyPair();
		adaptador = new BinancePayAdapter(
				"http://localhost:1", "api-key", SECRETO,
				Base64.getEncoder().encodeToString(claves.getPublic().getEncoded()),
				"COP", 1000,
				Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC));
	}

	// --- firma RSA del webhook real ---

	@Test
	void unaFirmaRsaLegitima_esValida() throws Exception {
		String carga = "{\"bizIdStr\":\"1\",\"bizStatus\":\"PAY_SUCCESS\",\"data\":\"{}\"}";

		assertThat(adaptador.firmaValida(carga, firmarComoBinance("1760000000000", "nonce-x", carga)))
				.isTrue();
	}

	@Test
	void siLaCargaCambiaDespuesDeFirmada_laFirmaNoValida() throws Exception {
		String original = "{\"bizIdStr\":\"1\",\"data\":\"{}\"}";
		String firma = firmarComoBinance("1760000000000", "nonce-x", original);

		assertThat(adaptador.firmaValida(original + " ", firma)).isFalse();
	}

	@Test
	void firmasIlegibles_sonInvalidas_jamasExcepcion() {
		String carga = "{}";

		assertThat(adaptador.firmaValida(carga, null)).isFalse();
		assertThat(adaptador.firmaValida(carga, "")).isFalse();
		assertThat(adaptador.firmaValida(carga, "sin-separadores")).isFalse();
		assertThat(adaptador.firmaValida(carga, "ts|nonce")).isFalse();
		assertThat(adaptador.firmaValida(carga, "ts|nonce|@@no-es-base64@@")).isFalse();
	}

	// --- firma interna de reconciliación (HU-015) ---

	@Test
	void laFirmaDeReconciliacion_soloValidaConNuestroSecreto() throws Exception {
		String carga = "{\"bizIdStr\":\"recon-ref-1\"}";
		// HMAC calculado aquí de forma independiente, no con el adaptador
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
		mac.init(new javax.crypto.spec.SecretKeySpec(
				SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
		String hmacCorrecto = java.util.HexFormat.of().withUpperCase()
				.formatHex(mac.doFinal(carga.getBytes(StandardCharsets.UTF_8)));

		assertThat(adaptador.firmaValida(carga,
				BinancePayAdapter.PREFIJO_FIRMA_RECONCILIACION + hmacCorrecto)).isTrue();
		// un HMAC con otro secreto (o corrupto) se rechaza
		assertThat(adaptador.firmaValida(carga,
				BinancePayAdapter.PREFIJO_FIRMA_RECONCILIACION + "AAAA")).isFalse();
	}

	// --- interpretación del payload ---

	@Test
	void unWebhookCompleto_seTraduceAlLenguajeDelDominio() {
		String carga = """
				{"bizType":"PAY","bizIdStr":"29383937493038367292","bizStatus":"PAY_SUCCESS",
				 "data":"{\\"merchantTradeNo\\":\\"ref-77\\",\\"totalFee\\":68000,\\"currency\\":\\"COP\\",\\"transactTime\\":1760350000123}"}
				""";

		WebhookDelProveedor webhook = adaptador.interpretarWebhook(carga);

		assertThat(webhook.idExternoEvento()).isEqualTo("29383937493038367292");
		assertThat(webhook.tipo()).isEqualTo("PAY_SUCCESS");
		assertThat(webhook.referencia().valor()).isEqualTo("ref-77");
		assertThat(webhook.monto()).isEqualTo(Dinero.cop(68000));
		assertThat(webhook.pagadoEn()).isEqualTo(Instant.ofEpochMilli(1760350000123L));
	}

	@Test
	void unWebhookIncompletoOMalformado_lanzaWebhookInvalido() {
		// sin bizIdStr (la clave de idempotencia): inaceptable
		assertThatThrownBy(() -> adaptador.interpretarWebhook(
				"{\"bizStatus\":\"PAY_SUCCESS\",\"data\":\"{}\"}"))
				.isInstanceOf(WebhookInvalidoException.class)
				.hasMessageContaining("bizIdStr");
		// data no es JSON
		assertThatThrownBy(() -> adaptador.interpretarWebhook(
				"{\"bizIdStr\":\"1\",\"bizStatus\":\"PAY_SUCCESS\",\"data\":\"no-json\"}"))
				.isInstanceOf(WebhookInvalidoException.class);
		// data sin la referencia del cobro
		assertThatThrownBy(() -> adaptador.interpretarWebhook(
				"{\"bizIdStr\":\"1\",\"bizStatus\":\"PAY_SUCCESS\",\"data\":\"{\\\"totalFee\\\":1}\"}"))
				.isInstanceOf(WebhookInvalidoException.class)
				.hasMessageContaining("merchantTradeNo");
		// totalFee no numérico
		assertThatThrownBy(() -> adaptador.interpretarWebhook(
				"{\"bizIdStr\":\"1\",\"bizStatus\":\"PAY_SUCCESS\","
						+ "\"data\":\"{\\\"merchantTradeNo\\\":\\\"r\\\",\\\"totalFee\\\":\\\"mil\\\"}\"}"))
				.isInstanceOf(WebhookInvalidoException.class)
				.hasMessageContaining("totalFee");
		// ni siquiera es JSON
		assertThatThrownBy(() -> adaptador.interpretarWebhook("<xml/>"))
				.isInstanceOf(WebhookInvalidoException.class);
	}

	// --- configuración ---

	@Test
	void unaClavePublicaCorrupta_impideArrancar() {
		// fail-fast: mejor no levantar que aceptar webhooks sin poder verificarlos
		assertThatThrownBy(() -> new BinancePayAdapter(
				"http://localhost:1", "api-key", SECRETO, "no-es-una-clave",
				"COP", 1000, Clock.systemUTC()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("clave pública");
	}

	private String firmarComoBinance(String timestamp, String nonce, String carga)
			throws Exception {
		Signature firmador = Signature.getInstance("SHA256withRSA");
		firmador.initSign(claves.getPrivate());
		firmador.update((timestamp + "\n" + nonce + "\n" + carga + "\n")
				.getBytes(StandardCharsets.UTF_8));
		return timestamp + "|" + nonce + "|"
				+ Base64.getEncoder().encodeToString(firmador.sign());
	}

}
