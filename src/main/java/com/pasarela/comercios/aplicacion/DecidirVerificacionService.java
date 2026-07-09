package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Implementa las decisiones de verificación del Admin (HU-005). El servicio
 * solo coordina: carga el comercio, delega la regla al dominio y persiste.
 */
@Service
public class DecidirVerificacionService implements DecidirVerificacionUseCase {

	private final ComercioRepositorio repositorio;
	private final Clock reloj;

	public DecidirVerificacionService(ComercioRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public Comercio decidir(ComandoDecisionVerificacion comando) {
		Comercio comercio = repositorio.buscarPorId(IdComercio.de(comando.comercioId()))
				.orElseThrow(() -> new ComercioNoEncontradoException(
						"No existe un comercio con id " + comando.comercioId()));
		aplicar(comercio, comando);
		return repositorio.guardar(comercio);
	}

	private void aplicar(Comercio comercio, ComandoDecisionVerificacion comando) {
		Instant ahora = reloj.instant();
		String decision = comando.decision() == null ? "" : comando.decision().trim().toUpperCase();
		switch (decision) {
			case "APROBAR" -> comercio.verificar(ahora);
			case "RECHAZAR" -> comercio.rechazar(comando.motivo(), ahora);
			case "SUSPENDER" -> comercio.suspender(comando.motivo(), ahora);
			case "REACTIVAR" -> comercio.reactivar(ahora);
			default -> throw new ComercioInvalidoException(
					"La decisión no es válida; use APROBAR, RECHAZAR, SUSPENDER o REACTIVAR");
		}
	}

}