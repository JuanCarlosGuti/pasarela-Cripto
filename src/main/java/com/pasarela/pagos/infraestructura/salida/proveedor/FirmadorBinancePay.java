package com.pasarela.pagos.infraestructura.salida.proveedor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Firma de peticiones salientes a Binance Pay (HU-021), según su contrato
 * público: HMAC-SHA512 sobre {@code timestamp + "\n" + nonce + "\n" +
 * cuerpo + "\n"}, expresado en hexadecimal MAYÚSCULAS, enviado en el header
 * {@code BinancePay-Signature}.
 */
final class FirmadorBinancePay {

	private static final String ALFANUMERICO =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom ALEATORIO = new SecureRandom();

	private FirmadorBinancePay() {
	}

	static String firmar(String timestamp, String nonce, String cuerpo, String secreto) {
		String carga = timestamp + "\n" + nonce + "\n" + cuerpo + "\n";
		try {
			Mac mac = Mac.getInstance("HmacSHA512");
			mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
			return HexFormat.of().withUpperCase()
					.formatHex(mac.doFinal(carga.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception excepcion) {
			throw new IllegalStateException("No fue posible firmar la petición a Binance Pay",
					excepcion);
		}
	}

	/** Nonce de 32 caracteres alfanuméricos, como exige el contrato del proveedor. */
	static String nonce() {
		StringBuilder nonce = new StringBuilder(32);
		for (int i = 0; i < 32; i++) {
			nonce.append(ALFANUMERICO.charAt(ALEATORIO.nextInt(ALFANUMERICO.length())));
		}
		return nonce.toString();
	}

}
