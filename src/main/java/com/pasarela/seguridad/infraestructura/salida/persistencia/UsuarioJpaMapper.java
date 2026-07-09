package com.pasarela.seguridad.infraestructura.salida.persistencia;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.seguridad.dominio.modelo.IdUsuario;
import com.pasarela.seguridad.dominio.modelo.RolUsuario;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import org.springframework.stereotype.Component;

/** Traducción bidireccional dominio ↔ JPA. */
@Component
public class UsuarioJpaMapper {

	UsuarioJpaEntity aEntidad(Usuario usuario) {
		return new UsuarioJpaEntity(
				usuario.id().valor(),
				usuario.email(),
				usuario.hashContrasena(),
				usuario.rol().name(),
				usuario.comercioId() == null ? null : usuario.comercioId().valor(),
				usuario.creadoEn());
	}

	Usuario aDominio(UsuarioJpaEntity entidad) {
		return Usuario.reconstituir(
				IdUsuario.de(entidad.id()),
				entidad.email(),
				entidad.hashContrasena(),
				RolUsuario.valueOf(entidad.rol()),
				entidad.comercioId() == null ? null : IdComercio.de(entidad.comercioId()),
				entidad.creadoEn());
	}

}
