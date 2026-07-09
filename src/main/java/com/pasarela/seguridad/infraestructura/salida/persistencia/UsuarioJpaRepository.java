package com.pasarela.seguridad.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, UUID> {

	Optional<UsuarioJpaEntity> findByEmail(String email);

}
