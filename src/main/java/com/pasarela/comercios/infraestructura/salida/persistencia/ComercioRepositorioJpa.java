package com.pasarela.comercios.infraestructura.salida.persistencia;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

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
	public PaginaDeComercios listar(int pagina, int tamano) {
		return aPagina(jpa.findAllByOrderByRegistradoEnDesc(PageRequest.of(pagina, tamano)));
	}

	@Override
	public PaginaDeComercios listarPorEstado(EstadoVerificacion estado, int pagina, int tamano) {
		return aPagina(jpa.findByEstadoVerificacionOrderByRegistradoEnDesc(
				estado.name(), PageRequest.of(pagina, tamano)));
	}

	private PaginaDeComercios aPagina(Page<ComercioJpaEntity> encontrados) {
		return new PaginaDeComercios(
				encontrados.getContent().stream().map(mapper::aDominio).toList(),
				encontrados.getTotalElements());
	}

}