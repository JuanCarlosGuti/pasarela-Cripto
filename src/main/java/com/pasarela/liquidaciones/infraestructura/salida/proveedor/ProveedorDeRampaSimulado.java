package com.pasarela.liquidaciones.infraestructura.salida.proveedor;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.compartido.dominio.puerto.ConsultorDeCuentaLiquidacion.DatosCuentaLiquidacion;
import com.pasarela.liquidaciones.dominio.puerto.salida.ProveedorDeRampaPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulador del proveedor de rampa (HU-025, ADR-006): mientras T-007 no
 * cierre con un proveedor real (Mural/Bitso), esto genera una conversión
 * ficticia pero con forma realista — tasa de cambio y comisión configurables,
 * referencia de proveedor y confirmación de la cuenta destino — para poder
 * mostrar el flujo completo sin custodiar un centavo real. Reemplazar por un
 * adaptador real es cero cambios en dominio (mismo puerto).
 */
@Component
public class ProveedorDeRampaSimulado implements ProveedorDeRampaPort {

	private final BigDecimal tasaCambioSimulada;
	private final Porcentaje comisionRampa;

	public ProveedorDeRampaSimulado(
			@Value("${pasarela.proveedores.rampa.tasa-cambio-simulada:4150}") BigDecimal tasaCambioSimulada,
			@Value("${pasarela.proveedores.rampa.comision-porcentual:0.8}") BigDecimal comisionRampa) {
		this.tasaCambioSimulada = tasaCambioSimulada;
		this.comisionRampa = new Porcentaje(comisionRampa);
	}

	@Override
	public ResultadoConversionRampa convertir(Dinero montoBruto, DatosCuentaLiquidacion cuentaDestino) {
		Dinero comision = montoBruto.porcentaje(comisionRampa);
		String referencia = "RAMPA-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		return new ResultadoConversionRampa(
				comision, tasaCambioSimulada, referencia, describir(cuentaDestino));
	}

	private static String describir(DatosCuentaLiquidacion cuenta) {
		String numero = cuenta.numero();
		String ultimosCuatro = numero.length() > 4 ? numero.substring(numero.length() - 4) : numero;
		return "%s ••••%s — %s".formatted(cuenta.tipoCuenta(), ultimosCuatro, cuenta.titular());
	}

}
