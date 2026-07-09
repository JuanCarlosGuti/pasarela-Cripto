package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta con aislamiento por dueño (HU-006): pedir un comercio ajeno
 * responde exactamente igual que pedir uno inexistente.
 */
@Service
public class ConsultarComercioService implements ConsultarComercioUseCase {

	private final ComercioRepositorio repositorio;

	public ConsultarComercioService(ComercioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public Comercio consultar(ComandoConsultarComercio comando) {
		if (comando.comercioDelSolicitante() != null
				&& !comando.comercioDelSolicitante().equals(comando.comercioId())) {
			throw noEncontrado();
		}
		return repositorio.buscarPorId(IdComercio.de(comando.comercioId()))
				.orElseThrow(ConsultarComercioService::noEncontrado);
	}

	private static ComercioNoEncontradoException noEncontrado() {
		return new ComercioNoEncontradoException("No existe un comercio con ese id");
	}

}
