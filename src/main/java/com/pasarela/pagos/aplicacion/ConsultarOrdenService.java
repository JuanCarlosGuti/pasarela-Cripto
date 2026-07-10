package com.pasarela.pagos.aplicacion;

import com.pasarela.pagos.dominio.excepcion.OrdenNoEncontradaException;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta del dueño con aislamiento (HU-009): pedir la orden de otro
 * comercio responde exactamente igual que pedir una inexistente.
 */
@Service
public class ConsultarOrdenService implements ConsultarOrdenUseCase {

	private final OrdenDePagoRepositorio repositorio;

	public ConsultarOrdenService(OrdenDePagoRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public OrdenDePago consultar(ComandoConsultarOrden comando) {
		OrdenDePago orden = repositorio.buscarPorId(IdOrden.de(comando.ordenId()))
				.orElseThrow(ConsultarOrdenService::noEncontrada);
		if (comando.comercioDelSolicitante() != null
				&& !orden.comercioId().valor().equals(comando.comercioDelSolicitante())) {
			throw noEncontrada();
		}
		return orden;
	}

	private static OrdenNoEncontradaException noEncontrada() {
		return new OrdenNoEncontradaException("No existe una orden con ese id");
	}

}
