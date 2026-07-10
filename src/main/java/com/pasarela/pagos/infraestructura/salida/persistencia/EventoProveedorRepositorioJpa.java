package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.pagos.dominio.modelo.EventoProveedor;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del evento crudo. Escribe en transacción PROPIA
 * (REQUIRES_NEW): el crudo debe sobrevivir aunque el procesamiento del
 * webhook falle o se rechace. La violación de la constraint única se
 * traduce a {@link EventoDuplicadoException} — la última línea de defensa
 * de la idempotencia ante webhooks concurrentes (ADR-004).
 */
@Repository
public class EventoProveedorRepositorioJpa implements EventoProveedorRepositorio {

	private final EventoProveedorJpaRepository jpa;
	private final EventoProveedorJpaMapper mapper;

	public EventoProveedorRepositorioJpa(EventoProveedorJpaRepository jpa,
			EventoProveedorJpaMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public EventoProveedor guardar(EventoProveedor evento) {
		try {
			return mapper.aDominio(jpa.saveAndFlush(mapper.aEntidad(evento)));
		} catch (DataIntegrityViolationException violacion) {
			throw new EventoDuplicadoException(
					"Ya existe el evento %s del proveedor %s".formatted(
							evento.idExternoEvento(), evento.proveedor()));
		}
	}

	@Override
	public boolean existe(String proveedor, String idExternoEvento) {
		return jpa.existsByProveedorAndIdExternoEvento(proveedor, idExternoEvento);
	}

}
