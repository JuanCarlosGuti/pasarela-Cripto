package com.pasarela.pagos.aplicacion;

import com.pasarela.pagos.dominio.excepcion.OrdenNoEncontradaException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarPagoPublicoUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta pública por referencia (HU-009): lo único que la página de pago
 * necesita — estado y monto. Nada del comercio sale de aquí.
 */
@Service
public class ConsultarPagoService implements ConsultarPagoPublicoUseCase {

	private final OrdenDePagoRepositorio repositorio;

	public ConsultarPagoService(OrdenDePagoRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public EstadoDePago consultar(String referencia) {
		OrdenDePago orden = repositorio.buscarPorReferencia(new ReferenciaPago(referencia))
				.orElseThrow(() -> new OrdenNoEncontradaException(
						"No existe un pago con esa referencia"));
		return new EstadoDePago(orden.estado().name(), orden.monto());
	}

}
