package com.pasarela.pagos.dominio.puerto.entrada;

import com.pasarela.compartido.dominio.modelo.Dinero;

/**
 * Caso de uso: la página de pago del pagador consulta por referencia
 * pública. Expone SOLO estado y monto — nada del comercio ni interno
 * (HU-009; el contrato se prueba).
 */
public interface ConsultarPagoPublicoUseCase {

	EstadoDePago consultar(String referencia);

	record EstadoDePago(String estado, Dinero monto) {
	}

}
