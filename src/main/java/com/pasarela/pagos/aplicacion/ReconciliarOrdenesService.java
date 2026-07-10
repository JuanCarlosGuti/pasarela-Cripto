package com.pasarela.pagos.aplicacion;

import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ComandoProcesarWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ProcesarWebhookUseCase.ResultadoWebhook;
import com.pasarela.pagos.dominio.puerto.entrada.ReconciliarOrdenesUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroConsultado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Job de reconciliación (HU-015, cierra el ADR-004): consulta activamente
 * al proveedor por las órdenes pendientes atascadas y, si reporta el pago,
 * lo confirma metiéndolo por {@link ProcesarWebhookUseCase} — LA MISMA
 * ruta idempotente del webhook, sin duplicar una línea de lógica. Si el
 * webhook llega casi a la vez, la idempotencia de HU-011 garantiza una
 * sola confirmación (el otro ve DUPLICADO).
 *
 * <p>Un proveedor caído se registra y se reintenta en el siguiente ciclo:
 * jamás tumba el job ni bloquea a las demás órdenes.</p>
 */
@Service
public class ReconciliarOrdenesService implements ReconciliarOrdenesUseCase {

	private static final Logger log = LoggerFactory.getLogger(ReconciliarOrdenesService.class);
	private static final int MAXIMO_POR_CICLO = 100;

	private final OrdenDePagoRepositorio repositorio;
	private final ProveedorDePagoPort proveedor;
	private final ProcesarWebhookUseCase procesarWebhook;
	private final Clock reloj;
	private final Duration umbralDeAtascada;
	private final String nombreDelProveedor;

	public ReconciliarOrdenesService(OrdenDePagoRepositorio repositorio,
			ProveedorDePagoPort proveedor, ProcesarWebhookUseCase procesarWebhook, Clock reloj,
			@Value("${pasarela.pagos.reconciliacion.minutos-atascada:5}") long minutosAtascada,
			@Value("${pasarela.pagos.reconciliacion.proveedor:simulado}") String nombreDelProveedor) {
		this.repositorio = repositorio;
		this.proveedor = proveedor;
		this.procesarWebhook = procesarWebhook;
		this.reloj = reloj;
		this.umbralDeAtascada = Duration.ofMinutes(minutosAtascada);
		this.nombreDelProveedor = nombreDelProveedor;
	}

	@Override
	public ResultadoReconciliacion reconciliar() {
		Instant limite = reloj.instant().minus(umbralDeAtascada);
		List<OrdenDePago> atascadas =
				repositorio.buscarPendientesCreadasAntesDe(limite, MAXIMO_POR_CICLO);
		int confirmadas = 0;
		int fallos = 0;
		for (OrdenDePago orden : atascadas) {
			try {
				Optional<CobroConsultado> pago =
						proveedor.consultarCobro(orden.referencia(), orden.monto());
				if (pago.isEmpty()) {
					continue;
				}
				ResultadoWebhook resultado = procesarWebhook.procesar(new ComandoProcesarWebhook(
						nombreDelProveedor, pago.get().cargaCruda(), pago.get().firma()));
				if (resultado == ResultadoWebhook.CONFIRMADO) {
					confirmadas++;
				}
			} catch (ProveedorDePagoNoDisponibleException caido) {
				fallos++;
				log.warn("El proveedor no respondió la consulta de la orden {}: {}",
						orden.id().valor(), caido.getMessage());
			}
		}
		return new ResultadoReconciliacion(atascadas.size(), confirmadas, fallos);
	}

}
