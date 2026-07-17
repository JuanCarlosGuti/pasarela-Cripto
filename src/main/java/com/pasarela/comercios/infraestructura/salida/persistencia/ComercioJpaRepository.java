package com.pasarela.comercios.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ComercioJpaRepository extends JpaRepository<ComercioJpaEntity, UUID> {

	Optional<ComercioJpaEntity> findByNit(String nit);

	List<ComercioJpaEntity> findAllByOrderByRegistradoEnDesc();

	List<ComercioJpaEntity> findByEstadoVerificacionOrderByRegistradoEnDesc(String estado);

}