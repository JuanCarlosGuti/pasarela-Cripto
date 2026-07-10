package com.pasarela.pagos.infraestructura.entrada.programacion;

import com.pasarela.pagos.dominio.puerto.entrada.ExpirarOrdenesVencidasUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ExpirarOrdenesVencidasUseCase.ResultadoExpiracion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el job de expiración (HU-014) en un intervalo fijo. Se activa por
 * propiedad: en la suite de tests está APAGADO (los tests invocan el caso
 * de uso directamente, con reloj controlado).
 */
@Component
@ConditionalOnProperty(name = "pasarela.pagos.expiracion.habilitada", havingValue = "true")
public class PlanificadorDeExpiracion {

	private static final Logger log = LoggerFactory.getLogger(PlanificadorDeExpiracion.class);

	private final ExpirarOrdenesVencidasUseCase expirarOrdenes;

	public PlanificadorDeExpiracion(ExpirarOrdenesVencidasUseCase expirarOrdenes) {
		this.expirarOrdenes = expirarOrdenes;
	}

	@Scheduled(fixedDelayString = "${pasarela.pagos.expiracion.milisegundos-intervalo:60000}")
	public void ejecutar() {
		ResultadoExpiracion resultado = expirarOrdenes.expirarVencidas();
		if (resultado.expiradas() > 0 || resultado.carrerasDetectadas() > 0) {
			log.info("Job de expiración: {} órdenes expiradas, {} carreras cedidas al pago",
					resultado.expiradas(), resultado.carrerasDetectadas());
		}
	}

}
