package com.pasarela.pagos.dominio.puerto.entrada;

/**
 * Caso de uso: procesar la notificación de pago del proveedor (HU-010).
 * Orden estricto del flujo (docs/05): firma → evento crudo → idempotencia →
 * interpretar → confirmar → notificar. Procesar N veces el mismo evento
 * produce exactamente el mismo resultado que una vez (ADR-004).
 */
public interface ProcesarWebhookUseCase {

	ResultadoWebhook procesar(ComandoProcesarWebhook comando);

	record ComandoProcesarWebhook(String proveedor, String cargaCruda, String firma) {
	}

	enum ResultadoWebhook {
		/** La orden pasó a PAGO_DETECTADO y el comercio fue notificado. */
		CONFIRMADO,
		/** Evento ya visto: 200 sin ningún efecto adicional. */
		DUPLICADO,
		/** No se pudo procesar automáticamente: queda para el Admin. */
		PARA_REVISION
	}

}
