package com.pasarela.pagos.dominio.puerto.salida;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;

import java.time.Instant;

/**
 * Puerto de salida hacia el proveedor de pagos (ADR-003): los proveedores
 * son adaptadores intercambiables de este puerto — simulado hoy, Binance
 * Pay en el Sprint 7 — con cero cambios en dominio y aplicación.
 *
 * <p>En el Sprint 4 crece con la validación de firma y la interpretación
 * de webhooks (docs/07).</p>
 */
public interface ProveedorDePagoPort {

	/**
	 * Registra el cobro en el proveedor y devuelve lo necesario para que el
	 * pagador pague: contenido del QR y deeplink.
	 *
	 * @throws com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException
	 *         si el proveedor falla; el caso de uso garantiza que entonces
	 *         NO queda ninguna orden persistida.
	 */
	CobroCreado crearCobro(SolicitudDeCobro solicitud);

	/** ¿La firma corresponde al proveedor? Primera puerta del webhook (HU-010). */
	boolean firmaValida(String cargaCruda, String firma);

	/**
	 * Traduce el payload del proveedor al lenguaje del dominio.
	 *
	 * @throws com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException
	 *         si el payload está malformado o incompleto.
	 */
	WebhookDelProveedor interpretarWebhook(String cargaCruda);

	record SolicitudDeCobro(ReferenciaPago referencia, Dinero monto, Instant expiraEn) {
	}

	record CobroCreado(String contenidoQr, String deeplink) {
	}

	record WebhookDelProveedor(String idExternoEvento, String tipo,
			ReferenciaPago referencia, Dinero monto, Instant pagadoEn) {
	}

}
