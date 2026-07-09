package com.pasarela.seguridad.infraestructura.salida.persistencia;

import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.salida.UsuarioRepositorio;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador JPA del puerto {@link UsuarioRepositorio}. */
@Repository
public class UsuarioRepositorioJpa implements UsuarioRepositorio {

	private final UsuarioJpaRepository jpa;
	private final UsuarioJpaMapper mapper;

	public UsuarioRepositorioJpa(UsuarioJpaRepository jpa, UsuarioJpaMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Usuario guardar(Usuario usuario) {
		return mapper.aDominio(jpa.saveAndFlush(mapper.aEntidad(usuario)));
	}

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return jpa.findByEmail(email).map(mapper::aDominio);
	}

}
