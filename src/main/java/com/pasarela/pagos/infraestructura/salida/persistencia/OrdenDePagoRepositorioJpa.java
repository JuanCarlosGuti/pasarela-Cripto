package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.IdOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link OrdenDePagoRepositorio}. */
@Repository
public class OrdenDePagoRepositorioJpa implements OrdenDePagoRepositorio {

	private final OrdenJpaRepository jpa;
	private final OrdenJpaMapper mapper;

	public OrdenDePagoRepositorioJpa(OrdenJpaRepository jpa, OrdenJpaMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public OrdenDePago guardar(OrdenDePago orden) {
		// saveAndFlush: las violaciones de constraint (referencia duplicada)
		// estallan aquí, no en un flush diferido lejos del culpable
		return mapper.aDominio(jpa.saveAndFlush(mapper.aEntidad(orden)));
	}

	@Override
	public Optional<OrdenDePago> buscarPorId(IdOrden id) {
		return jpa.findById(id.valor()).map(mapper::aDominio);
	}

	@Override
	public Optional<OrdenDePago> buscarPorReferencia(ReferenciaPago referencia) {
		return jpa.findByReferencia(referencia.valor()).map(mapper::aDominio);
	}

	@Override
	public List<OrdenDePago> buscarPendientesExpiradas(Instant ahora) {
		return jpa.findByEstadoAndExpiraEnBefore(EstadoOrden.PENDIENTE_PAGO.name(), ahora)
				.stream()
				.map(mapper::aDominio)
				.toList();
	}

}