package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComerciosUseCase;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;

/** Lista de comercios para la cola de verificación del Admin (HU-026). */
@Service
public class ConsultarComerciosService implements ConsultarComerciosUseCase {

	private final ComercioRepositorio repositorio;

	public ConsultarComerciosService(ComercioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public List<Comercio> listar(String estado) {
		if (estado == null || estado.isBlank()) {
			return repositorio.listar();
		}
		return repositorio.listarPorEstado(estadoValido(estado));
	}

	private static EstadoVerificacion estadoValido(String estado) {
		try {
			return EstadoVerificacion.valueOf(estado.trim().toUpperCase());
		} catch (IllegalArgumentException excepcion) {
			throw new ComercioInvalidoException(
					"El estado de verificación '%s' no existe".formatted(estado));
		}
	}

}
