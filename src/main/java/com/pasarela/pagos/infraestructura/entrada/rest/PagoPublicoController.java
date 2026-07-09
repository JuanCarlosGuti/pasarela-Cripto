package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.puerto.entrada.ConsultarPagoPublicoUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarPagoPublicoUseCase.EstadoDePago;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública para la página de pago (HU-009). El contrato expone
 * SOLO estado y monto: nada del comercio ni datos internos (probado).
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoPublicoController {

	private final ConsultarPagoPublicoUseCase consultarPago;

	public PagoPublicoController(ConsultarPagoPublicoUseCase consultarPago) {
		this.consultarPago = consultarPago;
	}

	@GetMapping("/{referencia}")
	public PagoPublicoResponse consultar(@PathVariable String referencia) {
		EstadoDePago estado = consultarPago.consultar(referencia);
		return new PagoPublicoResponse(estado.estado(), estado.monto().monto().longValue());
	}

	public record PagoPublicoResponse(String estado, long monto) {
	}

}
