package com.pasarela.liquidaciones.dominio.puerto.salida;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.puerto.ConsultorDeCuentaLiquidacion.DatosCuentaLiquidacion;

import java.math.BigDecimal;

/**
 * Puerto de salida hacia el proveedor de rampa (HU-025 / ADR-006): quien
 * convierte la cripto del pagador a COP y liquida directo a la cuenta del
 * comercio (REGLA DE ORO: la plataforma nunca toca ese dinero). Mismo patrón
 * hexagonal de {@code ProveedorDePagoPort} — agregar el proveedor real
 * (Mural/Bitso, T-007) es un adaptador nuevo, cero cambios en dominio.
 */
public interface ProveedorDeRampaPort {

	ResultadoConversionRampa convertir(Dinero montoBruto, DatosCuentaLiquidacion cuentaDestino);

	record ResultadoConversionRampa(
			Dinero comisionRampa,
			BigDecimal tasaCambioSimulada,
			String referenciaProveedor,
			String cuentaDestinoDescripcion) {
	}

}
