package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.excepcion.OrdenNoLiquidableException;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.puerto.LiquidadorDeOrdenes;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementa el puerto compartido {@link LiquidadorDeOrdenes}: las reglas
 * sobre las órdenes (dueño, estado, transición a LIQUIDADA) viven en su
 * contexto.
 */
@Service
public class LiquidarOrdenesService implements LiquidadorDeOrdenes {

	private final OrdenDePagoRepositorio repositorio;

	public LiquidarOrdenesService(OrdenDePagoRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrdenLiquidable> obtenerConvertidas(IdComercio comercioId, List<IdOrden> ordenes) {
		return ordenes.stream().map(id -> {
			OrdenDePago orden = repositorio.buscarPorId(id)
					.orElseThrow(() -> new OrdenNoLiquidableException(
							"No existe la orden " + id.valor()));
			if (!orden.comercioId().equals(comercioId)) {
				throw new OrdenNoLiquidableException(
						"La orden %s no pertenece al comercio indicado".formatted(id.valor()));
			}
			if (orden.estado() != EstadoOrden.CONVERTIDA) {
				throw new OrdenNoLiquidableException(
						"La orden %s no está CONVERTIDA (estado actual: %s)".formatted(
								id.valor(), orden.estado()));
			}
			return new OrdenLiquidable(orden.id(), orden.monto());
		}).toList();
	}

	@Override
	@Transactional
	public void marcarComoLiquidadas(List<IdOrden> ordenes, Instant ahora) {
		for (IdOrden id : ordenes) {
			OrdenDePago orden = repositorio.buscarPorId(id)
					.orElseThrow(() -> new OrdenNoLiquidableException(
							"No existe la orden " + id.valor()));
			orden.marcarComoLiquidada(ahora);
			repositorio.guardar(orden);
		}
	}

}
