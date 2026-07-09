package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioNoAutorizadoException;
import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.excepcion.LimiteExcedidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.AutorizadorDeCobros;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones.RegistroDeOperacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Implementa el puerto compartido {@link AutorizadorDeCobros}: la decisión
 * de si un comercio puede recibir un cobro vive aquí, en su contexto.
 * Un intento que excede los topes queda en la bitácora de operaciones
 * inusuales (HU-007) antes de rechazarse.
 */
@Service
public class AutorizarCobroService implements AutorizadorDeCobros {

	private final ComercioRepositorio repositorio;
	private final BitacoraDeOperaciones bitacora;
	private final Clock reloj;

	public AutorizarCobroService(ComercioRepositorio repositorio,
			BitacoraDeOperaciones bitacora, Clock reloj) {
		this.repositorio = repositorio;
		this.bitacora = bitacora;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public void autorizar(IdComercio comercioId, Dinero monto, Dinero acumuladoDelMes) {
		Comercio comercio = repositorio.buscarPorId(comercioId)
				.orElseThrow(() -> new ComercioNoEncontradoException(
						"No existe un comercio con id " + comercioId.valor()));
		if (!comercio.puedeCobrar()) {
			throw new ComercioNoAutorizadoException(
					"El comercio no puede cobrar: requiere estar verificado (estado actual: "
							+ comercio.estadoVerificacion() + ")");
		}
		try {
			comercio.validarCobro(monto, acumuladoDelMes);
		} catch (LimiteExcedidoException excepcion) {
			bitacora.registrar(new RegistroDeOperacion(
					"LIMITE_EXCEDIDO",
					comercio.id().valor().toString(),
					excepcion.getMessage(),
					reloj.instant()));
			throw excepcion;
		}
	}

}
