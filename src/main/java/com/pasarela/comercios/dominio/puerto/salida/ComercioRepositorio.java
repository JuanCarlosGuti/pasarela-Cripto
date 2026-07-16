package com.pasarela.comercios.dominio.puerto.salida;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.compartido.dominio.modelo.IdComercio;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: persistencia de comercios. */
public interface ComercioRepositorio {

	Comercio guardar(Comercio comercio);

	Optional<Comercio> buscarPorId(IdComercio id);

	/** Para impedir registros duplicados: el NIT es único en la plataforma. */
	Optional<Comercio> buscarPorNit(Nit nit);

	/** Todos los comercios, más recientes primero (HU-026, cola del admin). */
	List<Comercio> listar();

	/** Los comercios en un estado dado, más recientes primero (HU-026). */
	List<Comercio> listarPorEstado(EstadoVerificacion estado);

}