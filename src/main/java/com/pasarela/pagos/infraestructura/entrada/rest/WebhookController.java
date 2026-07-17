package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ComandoProcesarWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ResultadoWebhook;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrada de webhooks del proveedor (HU-010). No usa JWT: la autenticidad
 * la da la FIRMA del proveedor (primera puerta del flujo). Salvo firma
 * inválida (401), siempre responde 200 — un error 5xx provocaría
 * reintentos infinitos del proveedor (docs/07).
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

	private final ProcesarWebhookUseCase procesarWebhook;

	public WebhookController(ProcesarWebhookUseCase procesarWebhook) {
		this.procesarWebhook = procesarWebhook;
	}

	@PostMapping("/{proveedor}")
	public WebhookResponse recibir(
			@PathVariable String proveedor,
			@RequestHeader(name = "X-Firma", required = false) String firma,
			@RequestHeader(name = "BinancePay-Timestamp", required = false) String timestampBinance,
			@RequestHeader(name = "BinancePay-Nonce", required = false) String nonceBinance,
			@RequestHeader(name = "BinancePay-Signature", required = false) String firmaBinance,
			@RequestBody String cargaCruda) {
		ResultadoWebhook resultado = procesarWebhook.procesar(new ComandoProcesarWebhook(
				proveedor, cargaCruda,
				firmaEfectiva(firma, timestampBinance, nonceBinance, firmaBinance)));
		return new WebhookResponse(resultado.name());
	}

	/**
	 * Binance Pay firma con TRES headers (timestamp, nonce, firma RSA); el
	 * puerto recibe UNA firma, así que aquí se componen como
	 * {@code timestamp|nonce|firmaBase64} y el adaptador los descompone
	 * (HU-021). Los demás proveedores siguen usando X-Firma tal cual. Si
	 * falta alguna parte, la firma compuesta simplemente no validará (401) —
	 * nunca se lanza desde aquí.
	 */
	static String firmaEfectiva(String xFirma, String timestamp, String nonce,
			String firmaBinance) {
		if (firmaBinance == null || firmaBinance.isBlank()) {
			return xFirma;
		}
		return timestamp + "|" + nonce + "|" + firmaBinance;
	}

	public record WebhookResponse(String resultado) {
	}

}
