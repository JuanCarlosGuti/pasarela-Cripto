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
			@RequestBody String cargaCruda) {
		ResultadoWebhook resultado = procesarWebhook.procesar(
				new ComandoProcesarWebhook(proveedor, cargaCruda, firma));
		return new WebhookResponse(resultado.name());
	}

	public record WebhookResponse(String resultado) {
	}

}
