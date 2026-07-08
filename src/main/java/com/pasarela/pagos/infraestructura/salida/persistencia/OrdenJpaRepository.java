package com.pasarela.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrdenJpaRepository extends JpaRepository<OrdenJpaEntity, UUID> {

	Optional<OrdenJpaEntity> findByReferencia(String referencia);

	List<OrdenJpaEntity> findByEstadoAndExpiraEnBefore(String estado, Instant limite);

}
