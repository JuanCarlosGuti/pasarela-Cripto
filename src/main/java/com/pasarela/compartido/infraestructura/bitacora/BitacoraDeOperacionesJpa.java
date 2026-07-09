package com.pasarela.compartido.infraestructura.bitacora;

import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Adaptador JPA de la bitácora: cada registro es una fila inmutable.
 *
 * <p>Se escribe en transacción PROPIA (REQUIRES_NEW): un intento rechazado
 * (p. ej. límite excedido) hace rollback de la transacción del cobro, pero
 * la evidencia del intento DEBE sobrevivir — es el punto de la auditoría.</p>
 */
@Repository
public class BitacoraDeOperacionesJpa implements BitacoraDeOperaciones {

	private final BitacoraJpaRepository jpa;

	public BitacoraDeOperacionesJpa(BitacoraJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(RegistroDeOperacion registro) {
		jpa.save(new RegistroBitacoraJpaEntity(
				UUID.randomUUID(),
				registro.momento(),
				registro.tipo(),
				registro.actor(),
				registro.detalle()));
	}

}
