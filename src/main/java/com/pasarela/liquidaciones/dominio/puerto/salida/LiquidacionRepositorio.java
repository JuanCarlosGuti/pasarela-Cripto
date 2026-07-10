package com.pasarela.liquidaciones.dominio.puerto.salida;

import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.liquidaciones.dominio.modelo.IdLiquidacion;
import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia de liquidaciones. La unicidad de la orden
 * (una orden pertenece a UNA liquidación) vive como constraint en BD.
 */
public interface LiquidacionRepositorio {

	/**
	 * @throws com.pasarela.liquidaciones.dominio.excepcion.OrdenYaLiquidadaException
	 *         si alguna orden ya pertenece a otra liquidación (constraint).
	 */
	Liquidacion guardar(Liquidacion liquidacion);

	Optional<Liquidacion> buscarPorId(IdLiquidacion id);

	/** Pre-chequeo amable; la constraint en BD es la última defensa. */
	List<IdOrden> ordenesYaLiquidadas(List<IdOrden> ordenes);

}
