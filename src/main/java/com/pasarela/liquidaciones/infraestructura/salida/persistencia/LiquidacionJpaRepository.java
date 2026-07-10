package com.pasarela.liquidaciones.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface LiquidacionJpaRepository extends JpaRepository<LiquidacionJpaEntity, UUID> {

	@Query(value = "select orden_id from liquidacion_ordenes where orden_id in (:ordenes)",
			nativeQuery = true)
	List<UUID> ordenesYaLiquidadas(@Param("ordenes") Collection<UUID> ordenes);

}
