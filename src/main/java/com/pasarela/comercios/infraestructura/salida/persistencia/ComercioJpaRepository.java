package com.pasarela.comercios.infraestructura.salida.persistencia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ComercioJpaRepository extends JpaRepository<ComercioJpaEntity, UUID> {

	Optional<ComercioJpaEntity> findByNit(String nit);

	Page<ComercioJpaEntity> findAllByOrderByRegistradoEnDesc(Pageable paginacion);

	Page<ComercioJpaEntity> findByEstadoVerificacionOrderByRegistradoEnDesc(
			String estado, Pageable paginacion);

}