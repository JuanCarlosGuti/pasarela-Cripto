package com.pasarela.compartido.infraestructura.bitacora;

import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Adaptador JPA de la bitácora: cada registro es una fila inmutable. */
@Repository
public class BitacoraDeOperacionesJpa implements BitacoraDeOperaciones {

	private final BitacoraJpaRepository jpa;

	public BitacoraDeOperacionesJpa(BitacoraJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public void registrar(RegistroDeOperacion registro) {
		jpa.save(new RegistroBitacoraJpaEntity(
				UUID.randomUUID(),
				registro.momento(),
				registro.tipo(),
				registro.actor(),
				registro.detalle()));
	}

}
