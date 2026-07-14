package com.pasarela.pagos.aplicacion;

import com.pasarela.pagos.dominio.excepcion.ComprobanteNoDisponibleException;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase.ComandoConsultarOrden;
import com.pasarela.pagos.dominio.puerto.entrada.GenerarComprobanteUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Comprobante de venta (HU-020). Delega la carga en
 * {@link ConsultarOrdenUseCase} para heredar el aislamiento de HU-009 (una
 * orden ajena responde igual que una inexistente) y solo agrega la regla
 * propia: sin pago detectado no hay nada que soportar.
 */
@Service
public class GenerarComprobanteService implements GenerarComprobanteUseCase {

	private final ConsultarOrdenUseCase consultarOrden;
	private final Clock reloj;

	public GenerarComprobanteService(ConsultarOrdenUseCase consultarOrden, Clock reloj) {
		this.consultarOrden = consultarOrden;
		this.reloj = reloj;
	}

	@Override
	@Transactional(readOnly = true)
	public Comprobante generar(ComandoGenerarComprobante comando) {
		OrdenDePago orden = consultarOrden.consultar(new ComandoConsultarOrden(
				comando.ordenId(), comando.comercioDelSolicitante()));
		if (!orden.estado().esVentaEfectiva()) {
			throw new ComprobanteNoDisponibleException(
					"Una orden en estado %s no tiene comprobante: solo las pagadas o liquidadas"
							.formatted(orden.estado()));
		}
		return new Comprobante(
				orden.id().valor(),
				orden.referencia().valor(),
				orden.monto(),
				orden.estado(),
				orden.creadaEn(),
				orden.momentoDeTransicionA(EstadoOrden.PAGO_DETECTADO)
						.orElseThrow(() -> new IllegalStateException(
								"Orden pagada sin transición a PAGO_DETECTADO en el historial: "
										+ orden.id().valor())),
				orden.momentoDeTransicionA(EstadoOrden.LIQUIDADA).orElse(null),
				reloj.instant());
	}

}
