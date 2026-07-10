package com.pasarela.pagos.infraestructura.entrada.programacion;

import com.pasarela.pagos.dominio.puerto.entrada.ReconciliarOrdenesUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ReconciliarOrdenesUseCase.ResultadoReconciliacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara la reconciliación (HU-015) en un intervalo fijo. Activo por
 * propiedad; apagado en la suite de tests (se invoca el caso de uso
 * directamente).
 */
@Component
@ConditionalOnProperty(name = "pasarela.pagos.reconciliacion.habilitada", havingValue = "true")
public class PlanificadorDeReconciliacion {

	private static final Logger log = LoggerFactory.getLogger(PlanificadorDeReconciliacion.class);

	private final ReconciliarOrdenesUseCase reconciliarOrdenes;

	public PlanificadorDeReconciliacion(ReconciliarOrdenesUseCase reconciliarOrdenes) {
		this.reconciliarOrdenes = reconciliarOrdenes;
	}

	@Scheduled(fixedDelayString = "${pasarela.pagos.reconciliacion.milisegundos-intervalo:300000}")
	public void ejecutar() {
		ResultadoReconciliacion resultado = reconciliarOrdenes.reconciliar();
		if (resultado.consultadas() > 0) {
			log.info("Reconciliación: {} consultadas, {} confirmadas, {} fallos del proveedor",
					resultado.consultadas(), resultado.confirmadas(),
					resultado.fallosDelProveedor());
		}
	}

}
