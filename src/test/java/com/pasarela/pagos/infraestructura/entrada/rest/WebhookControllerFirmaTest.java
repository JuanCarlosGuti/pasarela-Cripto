package com.pasarela.pagos.infraestructura.entrada.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composición de la firma del webhook (HU-021): los tres headers de Binance
 * Pay viajan al puerto como una sola firma {@code timestamp|nonce|firma};
 * los proveedores con header único (X-Firma) pasan intactos.
 */
class WebhookControllerFirmaTest {

	@Test
	void sinHeadersDeBinance_pasaLaXFirmaTalCual() {
		assertThat(WebhookController.firmaEfectiva("abc123", null, null, null))
				.isEqualTo("abc123");
		assertThat(WebhookController.firmaEfectiva("abc123", "ts", "nonce", ""))
				.isEqualTo("abc123");
		assertThat(WebhookController.firmaEfectiva(null, null, null, null)).isNull();
	}

	@Test
	void conFirmaDeBinance_componeTimestampNonceYFirma() {
		assertThat(WebhookController.firmaEfectiva(null, "1760000000000", "nonce-x", "RSA=="))
				.isEqualTo("1760000000000|nonce-x|RSA==");
		// si Binance manda su firma, gana sobre una X-Firma espuria
		assertThat(WebhookController.firmaEfectiva("espuria", "t", "n", "f"))
				.isEqualTo("t|n|f");
	}

}
