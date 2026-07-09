package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.LimitesOperacion;
import com.pasarela.comercios.dominio.puerto.entrada.ActualizarLimitesUseCase;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones;
import com.pasarela.compartido.dominio.puerto.BitacoraDeOperaciones.RegistroDeOperacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Cambio de topes por el Admin (HU-007). El cambio queda auditado en la
 * bitácora con quién, cuándo y valores anterior/nuevo, dentro de la misma
 * transacción: sin bitácora no hay cambio.
 */
@Service
public class ActualizarLimitesService implements ActualizarLimitesUseCase {

	private final ComercioRepositorio repositorio;
	private final BitacoraDeOperaciones bitacora;
	private final Clock reloj;

	public ActualizarLimitesService(ComercioRepositorio repositorio,
			BitacoraDeOperaciones bitacora, Clock reloj) {
		this.repositorio = repositorio;
		this.bitacora = bitacora;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public Comercio actualizar(ComandoActualizarLimites comando) {
		Comercio comercio = repositorio.buscarPorId(IdComercio.de(comando.comercioId()))
				.orElseThrow(() -> new ComercioNoEncontradoException(
						"No existe un comercio con id " + comando.comercioId()));
		LimitesOperacion anteriores = comercio.limites();
		LimitesOperacion nuevos = new LimitesOperacion(
				Dinero.cop(comando.topePorTransaccion()),
				Dinero.cop(comando.topeMensual()));
		comercio.actualizarLimites(nuevos);
		Comercio guardado = repositorio.guardar(comercio);
		bitacora.registrar(new RegistroDeOperacion(
				"CAMBIO_DE_LIMITES",
				comando.actor(),
				"Comercio %s: tope por transacción %s → %s; tope mensual %s → %s".formatted(
						comercio.id().valor(),
						anteriores.topePorTransaccion().monto().toPlainString(),
						nuevos.topePorTransaccion().monto().toPlainString(),
						anteriores.topeMensual().monto().toPlainString(),
						nuevos.topeMensual().monto().toPlainString()),
				reloj.instant()));
		return guardado;
	}

}
