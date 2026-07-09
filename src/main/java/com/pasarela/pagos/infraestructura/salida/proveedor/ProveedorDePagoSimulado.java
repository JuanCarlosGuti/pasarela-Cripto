package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Proveedor simulado (T-006): adaptador de {@link ProveedorDePagoPort} para
 * desarrollar todo el MVP sin el sandbox de Binance.
 *
 * <ul>
 *   <li><b>Activación:</b> solo existe si
 *       {@code pasarela.proveedores.simulado.habilitado=true} (perfiles
 *       local y test). Producción jamás define la propiedad: el bean no se
 *       crea y, sin adaptador del puerto, el arranque falla rápido — mejor
 *       que un simulador aceptando cobros reales.</li>
 *   <li><b>Fallos configurables</b> para tests de resiliencia
 *       ({@code pasarela.proveedores.simulado.modo-fallo}): ERROR simula un
 *       500 del proveedor; TIMEOUT espera y falla como una conexión
 *       vencida.</li>
 * </ul>
 *
 * <p>La validación de firma y la interpretación de webhooks se agregan
 * cuando el puerto crezca en el Sprint 4 (HU-010).</p>
 */
@Component
@ConditionalOnProperty(name = "pasarela.proveedores.simulado.habilitado", havingValue = "true")
public class ProveedorDePagoSimulado implements ProveedorDePagoPort {

	public enum ModoDeFallo {
		NINGUNO, ERROR, TIMEOUT
	}

	private final ModoDeFallo modoDeFallo;
	private final long milisegundosTimeout;

	public ProveedorDePagoSimulado(
			@Value("${pasarela.proveedores.simulado.modo-fallo:NINGUNO}") ModoDeFallo modoDeFallo,
			@Value("${pasarela.proveedores.simulado.milisegundos-timeout:200}") long milisegundosTimeout) {
		this.modoDeFallo = modoDeFallo;
		this.milisegundosTimeout = milisegundosTimeout;
	}

	@Override
	public CobroCreado crearCobro(SolicitudDeCobro solicitud) {
		switch (modoDeFallo) {
			case ERROR -> throw new ProveedorDePagoNoDisponibleException(
					"El simulador está configurado en modo ERROR (500 del proveedor)");
			case TIMEOUT -> {
				esperar();
				throw new ProveedorDePagoNoDisponibleException(
						"El simulador está configurado en modo timeout ("
								+ milisegundosTimeout + " ms)");
			}
			case NINGUNO -> { }
		}
		String referencia = solicitud.referencia().valor();
		return new CobroCreado(
				"PAGOSIM|%s|%s".formatted(referencia, solicitud.monto().monto().toPlainString()),
				"pasarela-sim://pagar/" + referencia);
	}

	private void esperar() {
		try {
			Thread.sleep(milisegundosTimeout);
		} catch (InterruptedException interrupcion) {
			Thread.currentThread().interrupt();
		}
	}

}
