package com.pasarela.comercios.infraestructura.salida.persistencia;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import org.springframework.stereotype.Component;

/** Traducción bidireccional dominio ↔ JPA. */
@Component
public class ComercioJpaMapper {

	ComercioJpaEntity aEntidad(Comercio comercio) {
		return new ComercioJpaEntity(
				comercio.id().valor(),
				comercio.razonSocial(),
				comercio.nit().completo(),
				comercio.estadoVerificacion().name(),
				comercio.cuentaLiquidacion().tipo().name(),
				comercio.cuentaLiquidacion().numero(),
				comercio.cuentaLiquidacion().titular(),
				comercio.registradoEn());
	}

	Comercio aDominio(ComercioJpaEntity entidad) {
		return Comercio.reconstituir(
				IdComercio.de(entidad.id()),
				entidad.razonSocial(),
				Nit.de(entidad.nit()),
				new CuentaLiquidacion(
						TipoCuenta.valueOf(entidad.cuentaTipo()),
						entidad.cuentaNumero(),
						entidad.cuentaTitular()),
				EstadoVerificacion.valueOf(entidad.estadoVerificacion()),
				entidad.registradoEn());
	}

}