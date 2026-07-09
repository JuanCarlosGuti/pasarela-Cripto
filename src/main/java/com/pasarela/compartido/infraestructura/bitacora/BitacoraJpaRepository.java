package com.pasarela.compartido.infraestructura.bitacora;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface BitacoraJpaRepository extends JpaRepository<RegistroBitacoraJpaEntity, UUID> {

}
