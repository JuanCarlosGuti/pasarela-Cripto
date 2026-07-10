package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.pagos.dominio.modelo.EventoProveedor;
import com.pasarela.pagos.dominio.modelo.IdEventoProveedor;
import org.springframework.stereotype.Component;

/** Traducción bidireccional dominio ↔ JPA. */
@Component
public class EventoProveedorJpaMapper {

	EventoProveedorJpaEntity aEntidad(EventoProveedor evento) {
		return new EventoProveedorJpaEntity(
				evento.id().valor(),
				evento.proveedor(),
				evento.idExternoEvento(),
				evento.tipo(),
				evento.cargaCruda(),
				evento.firmaValida(),
				evento.procesado(),
				evento.notaRevision(),
				evento.recibidoEn());
	}

	EventoProveedor aDominio(EventoProveedorJpaEntity entidad) {
		return EventoProveedor.reconstituir(
				IdEventoProveedor.de(entidad.id()),
				entidad.proveedor(),
				entidad.idExternoEvento(),
				entidad.tipo(),
				entidad.cargaCruda(),
				entidad.firmaValida(),
				entidad.procesado(),
				entidad.notaRevision(),
				entidad.recibidoEn());
	}

}
