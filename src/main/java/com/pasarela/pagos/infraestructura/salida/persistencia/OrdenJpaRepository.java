package com.pasarela.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrdenJpaRepository extends JpaRepository<OrdenJpaEntity, UUID> {

	Optional<OrdenJpaEntity> findByReferencia(String referencia);

	List<OrdenJpaEntity> findByEstadoAndExpiraEnBefore(String estado, Instant limite,
			org.springframework.data.domain.Pageable paginacion);

	List<OrdenJpaEntity> findByEstadoAndCreadaEnBefore(String estado, Instant limite,
			org.springframework.data.domain.Pageable paginacion);

	@Query("""
			select coalesce(sum(o.monto), 0)
			from OrdenJpaEntity o
			where o.comercioId = :comercioId
			  and o.creadaEn >= :desde and o.creadaEn < :hasta
			  and o.estado in :estados
			""")
	BigDecimal sumarMontos(
			@Param("comercioId") UUID comercioId,
			@Param("desde") Instant desde,
			@Param("hasta") Instant hasta,
			@Param("estados") Collection<String> estados);

}
