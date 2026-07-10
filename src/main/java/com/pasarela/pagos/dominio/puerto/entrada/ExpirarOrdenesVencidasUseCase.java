package com.pasarela.pagos.dominio.puerto.entrada;

/**
 * Caso de uso del job de expiración (HU-014): las órdenes PENDIENTE_PAGO
 * cuya ventana venció pasan a EXPIRADA. Idempotente (correrlo dos veces
 * seguidas produce lo mismo) y por lotes (jamás toda la tabla en memoria).
 * Si una orden pierde la carrera contra un pago simultáneo, se salta
 * limpiamente: el pago gana.
 */
public interface ExpirarOrdenesVencidasUseCase {

	ResultadoExpiracion expirarVencidas();

	record ResultadoExpiracion(int expiradas, int carrerasDetectadas) {
	}

}
