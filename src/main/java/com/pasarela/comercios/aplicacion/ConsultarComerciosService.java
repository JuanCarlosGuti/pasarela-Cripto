package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComerciosUseCase;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import org.springframework.stereotype.Service;

/** Cola de verificación del Admin, paginada (HU-026/HU-027). */
@Service
public class ConsultarComerciosService implements ConsultarComerciosUseCase {

	private static final int TAMANO_MAXIMO = 100;

	private final ComercioRepositorio repositorio;

	public ConsultarComerciosService(ComercioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public ConsultarComerciosUseCase.PaginaDeComercios listar(String estado, int pagina, int tamano) {
		int paginaValida = Math.max(0, pagina);
		int tamanoValido = Math.min(Math.max(1, tamano), TAMANO_MAXIMO);
		ComercioRepositorio.PaginaDeComercios encontrados = (estado == null || estado.isBlank())
				? repositorio.listar(paginaValida, tamanoValido)
				: repositorio.listarPorEstado(estadoValido(estado), paginaValida, tamanoValido);
		return new ConsultarComerciosUseCase.PaginaDeComercios(
				encontrados.comercios(), encontrados.totalElementos(), paginaValida, tamanoValido);
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
