package com.pasarela.pagos.infraestructura.salida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface EventoProveedorJpaRepository extends JpaRepository<EventoProveedorJpaEntity, UUID> {

	boolean existsByProveedorAndIdExternoEvento(String proveedor, String idExternoEvento);

}
