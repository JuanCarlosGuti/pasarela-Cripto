package com.pasarela.pagos.infraestructura.salida.notificacion;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.salida.NotificadorDeComercios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Notificador provisional: deja el "PAGADO ✓" en el log. HU-013 lo
 * reemplaza por el mecanismo que consumirá el frontend (SSE o polling).
 * Nunca registra montos ni datos del comercio, solo ids.
 */
@Component
public class NotificadorPorLog implements NotificadorDeComercios {

	private static final Logger log = LoggerFactory.getLogger(NotificadorPorLog.class);

	@Override
	public void pagoDetectado(OrdenDePago orden) {
		log.info("PAGADO: la orden {} del comercio {} pasó a {}",
				orden.id().valor(), orden.comercioId().valor(), orden.estado());
	}

}
