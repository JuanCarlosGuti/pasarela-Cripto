package com.pasarela.liquidaciones.infraestructura.salida.persistencia;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.modelo.Moneda;
import com.pasarela.liquidaciones.dominio.modelo.EstadoLiquidacion;
import com.pasarela.liquidaciones.dominio.modelo.IdLiquidacion;
import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import org.springframework.stereotype.Component;

/** Traducción bidireccional dominio ↔ JPA. */
@Component
public class LiquidacionJpaMapper {

	LiquidacionJpaEntity aEntidad(Liquidacion liquidacion) {
		return new LiquidacionJpaEntity(
				liquidacion.id().valor(),
				liquidacion.comercioId().valor(),
				liquidacion.montoBruto().monto(),
				liquidacion.comisionPlataforma().monto(),
				liquidacion.montoNetoComercio().monto(),
				liquidacion.referenciaProveedor(),
				liquidacion.estado().name(),
				liquidacion.liquidadaEn(),
				liquidacion.ordenes().stream().map(IdOrden::valor).toList(),
				liquidacion.detalleDiscrepancia());
	}

	Liquidacion aDominio(LiquidacionJpaEntity entidad) {
		return Liquidacion.reconstituir(
				IdLiquidacion.de(entidad.id()),
				IdComercio.de(entidad.comercioId()),
				entidad.ordenes().stream().map(IdOrden::de).toList(),
				new Dinero(entidad.montoBruto(), Moneda.COP),
				new Dinero(entidad.comisionPlataforma(), Moneda.COP),
				new Dinero(entidad.montoNetoComercio(), Moneda.COP),
				entidad.referenciaProveedor(),
				EstadoLiquidacion.valueOf(entidad.estado()),
				entidad.liquidadaEn(),
				entidad.detalleDiscrepancia());
	}

}
