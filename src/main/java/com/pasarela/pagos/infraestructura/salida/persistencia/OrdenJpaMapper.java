package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.Moneda;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.IdOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.modelo.TransicionEstado;
import org.springframework.stereotype.Component;

/** Traducción bidireccional dominio ↔ JPA, sin pérdidas. */
@Component
public class OrdenJpaMapper {

	OrdenJpaEntity aEntidad(OrdenDePago orden) {
		return new OrdenJpaEntity(
				orden.id().valor(),
				orden.comercioId().valor(),
				orden.monto().monto(),
				orden.monto().moneda().name(),
				orden.referencia().valor(),
				orden.estado().name(),
				orden.creadaEn(),
				orden.expiraEn(),
				orden.historial().stream()
						.map(transicion -> new TransicionEstadoEmbeddable(
								transicion.desde().name(),
								transicion.hacia().name(),
								transicion.momento(),
								transicion.motivo()))
						.toList(),
				orden.version());
	}

	OrdenDePago aDominio(OrdenJpaEntity entidad) {
		return OrdenDePago.reconstituir(
				IdOrden.de(entidad.id()),
				IdComercio.de(entidad.comercioId()),
				new Dinero(entidad.monto(), Moneda.valueOf(entidad.moneda())),
				new ReferenciaPago(entidad.referencia()),
				entidad.creadaEn(),
				entidad.expiraEn(),
				EstadoOrden.valueOf(entidad.estado()),
				entidad.transiciones().stream()
						.map(transicion -> new TransicionEstado(
								EstadoOrden.valueOf(transicion.desde()),
								EstadoOrden.valueOf(transicion.hacia()),
								transicion.momento(),
								transicion.motivo()))
						.toList(),
				entidad.version());
	}

}