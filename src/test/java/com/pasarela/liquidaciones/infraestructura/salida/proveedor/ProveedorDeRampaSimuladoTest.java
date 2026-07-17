package com.pasarela.liquidaciones.infraestructura.salida.proveedor;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.puerto.ConsultorDeCuentaLiquidacion.DatosCuentaLiquidacion;
import com.pasarela.liquidaciones.dominio.puerto.salida.ProveedorDeRampaPort.ResultadoConversionRampa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProveedorDeRampaSimuladoTest {

	private ProveedorDeRampaSimulado simulador;

	@BeforeEach
	void configurar() {
		simulador = new ProveedorDeRampaSimulado(
				new BigDecimal("4150"), new BigDecimal("0.8"));
	}

	@Test
	void convierte_cobrandoLaComisionDeRampaSobreElBruto() {
		DatosCuentaLiquidacion cuenta = new DatosCuentaLiquidacion(
				"NEQUI", "3001234567", "Doña Rosa");

		ResultadoConversionRampa resultado = simulador.convertir(Dinero.cop(100_000), cuenta);

		// 0.8% de 100.000 = 800
		assertThat(resultado.comisionRampa()).isEqualTo(Dinero.cop(800));
	}

	@Test
	void devuelveLaTasaDeCambioConfigurada() {
		DatosCuentaLiquidacion cuenta = new DatosCuentaLiquidacion(
				"NEQUI", "3001234567", "Doña Rosa");

		ResultadoConversionRampa resultado = simulador.convertir(Dinero.cop(100_000), cuenta);

		assertThat(resultado.tasaCambioSimulada()).isEqualByComparingTo("4150");
	}

	@Test
	void laReferenciaTieneFormatoDeSimulacion_yNuncaSeRepite() {
		DatosCuentaLiquidacion cuenta = new DatosCuentaLiquidacion(
				"NEQUI", "3001234567", "Doña Rosa");

		ResultadoConversionRampa uno = simulador.convertir(Dinero.cop(100_000), cuenta);
		ResultadoConversionRampa otro = simulador.convertir(Dinero.cop(100_000), cuenta);

		assertThat(uno.referenciaProveedor()).startsWith("RAMPA-SIM-");
		assertThat(uno.referenciaProveedor()).isNotEqualTo(otro.referenciaProveedor());
	}

	@Test
	void laDescripcionDelDestinoEnmascaraElNumeroDeCuenta() {
		DatosCuentaLiquidacion cuenta = new DatosCuentaLiquidacion(
				"NEQUI", "3001234567", "Doña Rosa");

		ResultadoConversionRampa resultado = simulador.convertir(Dinero.cop(100_000), cuenta);

		assertThat(resultado.cuentaDestinoDescripcion())
				.contains("NEQUI").contains("4567").contains("Doña Rosa")
				.doesNotContain("3001234567");
	}

}
