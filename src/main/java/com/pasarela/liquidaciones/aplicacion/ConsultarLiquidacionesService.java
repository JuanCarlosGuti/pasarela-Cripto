package com.pasarela.liquidaciones.aplicacion;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import com.pasarela.liquidaciones.dominio.puerto.entrada.ConsultarLiquidacionesUseCase;
import com.pasarela.liquidaciones.dominio.puerto.salida.LiquidacionRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultarLiquidacionesService implements ConsultarLiquidacionesUseCase {

	private final LiquidacionRepositorio repositorio;

	public ConsultarLiquidacionesService(LiquidacionRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public List<Liquidacion> listar(UUID comercioId) {
		return repositorio.listarPorComercio(IdComercio.de(comercioId));
	}

}
