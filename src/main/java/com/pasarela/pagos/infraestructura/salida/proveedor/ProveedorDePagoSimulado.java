package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import org.springframework.stereotype.Component;

/**
 * Proveedor simulado (adaptador de ProveedorDePagoPort): QR determinista
 * para desarrollar todo el MVP sin depender del sandbox de Binance.
 *
 * <p>T-006 lo completa: fallos configurables (timeout, 500, firma
 * inválida), activación por propiedad y prueba de que el bean NO existe
 * con perfil prod.</p>
 */
@Component
public class ProveedorDePagoSimulado implements ProveedorDePagoPort {

	@Override
	public CobroCreado crearCobro(SolicitudDeCobro solicitud) {
		String referencia = solicitud.referencia().valor();
		return new CobroCreado(
				"PAGOSIM|%s|%s".formatted(referencia, solicitud.monto().monto().toPlainString()),
				"pasarela-sim://pagar/" + referencia);
	}

}
