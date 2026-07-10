package com.pasarela.liquidaciones.infraestructura.salida.persistencia;

import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.liquidaciones.dominio.excepcion.OrdenYaLiquidadaException;
import com.pasarela.liquidaciones.dominio.modelo.IdLiquidacion;
import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import com.pasarela.liquidaciones.dominio.puerto.salida.LiquidacionRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA de liquidaciones. La constraint única sobre orden_id es la
 * última defensa contra la doble liquidación: su violación se traduce a la
 * excepción de dominio.
 */
@Repository
public class LiquidacionRepositorioJpa implements LiquidacionRepositorio {

	private final LiquidacionJpaRepository jpa;
	private final LiquidacionJpaMapper mapper;

	public LiquidacionRepositorioJpa(LiquidacionJpaRepository jpa, LiquidacionJpaMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Liquidacion guardar(Liquidacion liquidacion) {
		try {
			return mapper.aDominio(jpa.saveAndFlush(mapper.aEntidad(liquidacion)));
		} catch (DataIntegrityViolationException violacion) {
			throw new OrdenYaLiquidadaException(
					"Alguna orden de la liquidación ya pertenece a otra (constraint en BD)");
		}
	}

	@Override
	public Optional<Liquidacion> buscarPorId(IdLiquidacion id) {
		return jpa.findById(id.valor()).map(mapper::aDominio);
	}

	@Override
	public List<IdOrden> ordenesYaLiquidadas(List<IdOrden> ordenes) {
		return jpa.ordenesYaLiquidadas(ordenes.stream().map(IdOrden::valor).toList())
				.stream().map(IdOrden::de).toList();
	}

}
