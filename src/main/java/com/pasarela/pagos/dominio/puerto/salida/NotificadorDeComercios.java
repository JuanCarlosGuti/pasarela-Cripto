package com.pasarela.pagos.dominio.puerto.salida;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;

/**
 * Puerto de salida: avisar al comercio que su cobro fue pagado (HU-013).
 * La notificación es best-effort: si falla, la confirmación de la orden NO
 * se revierte — la fuente de verdad es el estado de la orden.
 */
public interface NotificadorDeComercios {

	void pagoDetectado(OrdenDePago orden);

}
