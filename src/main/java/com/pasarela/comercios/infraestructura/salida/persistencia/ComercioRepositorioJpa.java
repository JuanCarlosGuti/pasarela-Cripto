package com.pasarela.comercios.infraestructura.salida.persistencia;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link ComercioRepositorio}. */
@Repository
public class ComercioRepositorioJpa implements ComercioRepositorio {

	private final ComercioJpaRepository jpa;
	private final ComercioJpaMapper mapper;

	public ComercioRepositorioJpa(ComercioJpaRepository jpa, ComercioJpaMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Comercio guardar(Comercio comercio) {
		return mapper.aDominio(jpa.saveAndFlush(mapper.aEntidad(comercio)));
	}

	@Override
	public Optional<Comercio> buscarPorId(IdComercio id) {
		return jpa.findById(id.valor()).map(mapper::aDominio);
	}

	@Override
	public Optional<Comercio> buscarPorNit(Nit nit) {
		return jpa.findByNit(nit.completo()).map(mapper::aDominio);
	}

	@Override
	public List<Comercio> listar() {
		return jpa.findAllByOrderByRegistradoEnDesc().stream().map(mapper::aDominio).toList();
	}

	@Override
	public List<Comercio> listarPorEstado(EstadoVerificacion estado) {
		return jpa.findByEstadoVerificacionOrderByRegistradoEnDesc(estado.name())
				.stream().map(mapper::aDominio).toList();
	}

}