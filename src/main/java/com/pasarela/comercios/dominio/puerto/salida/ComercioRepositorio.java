package com.pasarela.comercios.dominio.puerto.salida;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.compartido.dominio.modelo.IdComercio;

import java.util.Optional;

/** Puerto de salida: persistencia de comercios. */
public interface ComercioRepositorio {

	Comercio guardar(Comercio comercio);

	Optional<Comercio> buscarPorId(IdComercio id);

	/** Para impedir registros duplicados: el NIT es único en la plataforma. */
	Optional<Comercio> buscarPorNit(Nit nit);

}