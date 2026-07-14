package com.pasarela.pagos.infraestructura.salida.proveedor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Firma de peticiones a Binance Pay (HU-021): HMAC-SHA512 sobre
 * {@code timestamp\nnonce\ncuerpo\n} en hex MAYÚSCULAS — el contrato
 * documentado del proveedor. El vector esperado se calculó con una
 * implementación independiente (Node crypto), no con esta clase.
 */
class FirmadorBinancePayTest {

	@Test
	void firma_conElContratoDeBinance_coincideConElVectorIndependiente() {
		String firma = FirmadorBinancePay.firmar(
				"1699999999999",
				"nonce-de-prueba-32-caracteres-xx",
				"{\"merchantTradeNo\":\"ref-1\"}",
				"secreto-de-prueba");

		assertThat(firma).isEqualTo(
				"E508DDA40B9F5A23614EEC5A99A9FE5074003D712525F16332887D5926AE98ED"
						+ "9E45BD36EF5329CD16B05CB55A688160239485310830E580795734F442F728E1");
	}

	@Test
	void laFirma_cambiaSiCambiaCualquierParte() {
		String base = FirmadorBinancePay.firmar("t", "n", "cuerpo", "secreto");

		assertThat(FirmadorBinancePay.firmar("t2", "n", "cuerpo", "secreto")).isNotEqualTo(base);
		assertThat(FirmadorBinancePay.firmar("t", "n2", "cuerpo", "secreto")).isNotEqualTo(base);
		assertThat(FirmadorBinancePay.firmar("t", "n", "cuerpo2", "secreto")).isNotEqualTo(base);
		assertThat(FirmadorBinancePay.firmar("t", "n", "cuerpo", "secreto2")).isNotEqualTo(base);
	}

	@Test
	void elNonce_tiene32CaracteresAlfanumericosYEsDistintoCadaVez() {
		String uno = FirmadorBinancePay.nonce();
		String otro = FirmadorBinancePay.nonce();

		assertThat(uno).hasSize(32).matches("[a-zA-Z0-9]{32}");
		assertThat(otro).hasSize(32);
		assertThat(uno).isNotEqualTo(otro);
	}

}
