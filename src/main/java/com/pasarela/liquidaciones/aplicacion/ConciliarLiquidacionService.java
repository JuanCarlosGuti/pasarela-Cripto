package com.pasarela.liquidaciones.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones.RegistroDeOperacion;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionNoEncontradaException;
import com.pasarela.liquidaciones.dominio.modelo.EstadoLiquidacion;
import com.pasarela.liquidaciones.dominio.modelo.IdLiquidacion;
import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import com.pasarela.liquidaciones.dominio.modelo.ReporteDelProveedor;
import com.pasarela.liquidaciones.dominio.puerto.entrada.ConciliarLiquidacionUseCase;
import com.pasarela.liquidaciones.dominio.puerto.salida.LiquidacionRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Concilia la liquidación (HU-017): la comparación vive en el dominio; el
 * servicio persiste el resultado y, si hay discrepancia, alerta al Admin
 * en la bitácora — la discrepancia jamás pasa inadvertida.
 */
@Service
public class ConciliarLiquidacionService implements ConciliarLiquidacionUseCase {

	private final LiquidacionRepositorio repositorio;
	private final BitacoraDeOperaciones bitacora;
	private final Clock reloj;

	public ConciliarLiquidacionService(LiquidacionRepositorio repositorio,
			BitacoraDeOperaciones bitacora, Clock reloj) {
		this.repositorio = repositorio;
		this.bitacora = bitacora;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public Liquidacion conciliar(ComandoConciliar comando) {
		Liquidacion liquidacion = repositorio.buscarPorId(IdLiquidacion.de(comando.liquidacionId()))
				.orElseThrow(() -> new LiquidacionNoEncontradaException(
						"No existe una liquidación con id " + comando.liquidacionId()));
		liquidacion.conciliar(new ReporteDelProveedor(
				Dinero.cop(comando.montoBrutoReportado()),
				comando.ordenesReportadas().stream().map(IdOrden::de).toList()));
		Liquidacion guardada = repositorio.guardar(liquidacion);
		if (guardada.estado() == EstadoLiquidacion.DISCREPANCIA) {
			bitacora.registrar(new RegistroDeOperacion(
					"DISCREPANCIA_CONCILIACION",
					comando.actor(),
					"Liquidación %s en discrepancia: %s".formatted(
							guardada.id().valor(), guardada.detalleDiscrepancia()),
					reloj.instant()));
		}
		return guardada;
	}

}
