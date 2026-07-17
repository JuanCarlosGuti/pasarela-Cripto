package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.ConsultorDeCuentaLiquidacion;
import org.springframework.stereotype.Service;

/**
 * Implementa el puerto compartido {@link ConsultorDeCuentaLiquidacion}: la
 * cuenta de liquidación vive en `comercios`, su contexto dueño.
 */
@Service
public class ConsultarCuentaLiquidacionService implements ConsultorDeCuentaLiquidacion {

	private final ComercioRepositorio repositorio;

	public ConsultarCuentaLiquidacionService(ComercioRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public DatosCuentaLiquidacion obtener(IdComercio comercioId) {
		Comercio comercio = repositorio.buscarPorId(comercioId)
				.orElseThrow(() -> new ComercioNoEncontradoException(
						"No existe un comercio con id " + comercioId.valor()));
		CuentaLiquidacion cuenta = comercio.cuentaLiquidacion();
		return new DatosCuentaLiquidacion(cuenta.tipo().name(), cuenta.numero(), cuenta.titular());
	}

}
