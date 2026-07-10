package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.compartido.dominio.puerto.LiquidadorDeOrdenes.OrdenLiquidable;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiquidacionTest {

	private static final Instant AHORA = Instant.parse("2026-07-10T18:00:00Z");
	private static final Porcentaje TASA = Porcentaje.de("2.5");
	private static final IdComercio COMERCIO = IdComercio.generar();

	@Nested
	class Registro {

		@Test
		void agrupaLasOrdenes_yCalculaBrutoComisionYNeto() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO, List.of(
							orden(40000), orden(60000)),
					TASA, "liq-prov-001", AHORA);

			assertThat(liquidacion.id()).isNotNull();
			assertThat(liquidacion.ordenes()).hasSize(2);
			assertThat(liquidacion.montoBruto()).isEqualTo(Dinero.cop(100000));
			assertThat(liquidacion.comisionPlataforma()).isEqualTo(Dinero.cop(2500));
			assertThat(liquidacion.montoNetoComercio()).isEqualTo(Dinero.cop(97500));
			assertThat(liquidacion.estado()).isEqualTo(EstadoLiquidacion.REGISTRADA);
			assertThat(liquidacion.referenciaProveedor()).isEqualTo("liq-prov-001");
			assertThat(liquidacion.liquidadaEn()).isEqualTo(AHORA);
		}

		@Test
		void conMontosQueForzanRedondeo_todoCuadraAlCentavo() {
			// Criterio literal de HU-016: 3 × 33.333 = 99.999; el 2.5% es
			// 2.499,975 → redondeo bancario a 2.500; neto por diferencia
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO, List.of(
							orden(33333), orden(33333), orden(33333)),
					TASA, "liq-prov-002", AHORA);

			assertThat(liquidacion.montoBruto()).isEqualTo(Dinero.cop(99999));
			assertThat(liquidacion.comisionPlataforma()).isEqualTo(Dinero.cop(2500));
			assertThat(liquidacion.montoNetoComercio()).isEqualTo(Dinero.cop(97499));
			// la propiedad inviolable: comisión + neto = bruto, SIEMPRE
			assertThat(liquidacion.comisionPlataforma()
					.sumar(liquidacion.montoNetoComercio()))
					.isEqualTo(liquidacion.montoBruto());
		}

		@Test
		void brutoEsExactamenteLaSumaDeLasOrdenes() {
			List<OrdenLiquidable> ordenes = List.of(
					orden(17), orden(23), orden(41), orden(999983));

			Liquidacion liquidacion = Liquidacion.registrar(
					COMERCIO, ordenes, TASA, "liq-prov-003", AHORA);

			assertThat(liquidacion.montoBruto()).isEqualTo(Dinero.cop(17 + 23 + 41 + 999983));
		}

		@Test
		void sinOrdenes_lanzaExcepcion() {
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, List.of(), TASA, "liq-prov-004", AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class)
					.hasMessageContaining("al menos una orden");
		}

		@Test
		void conOrdenesRepetidasEnLaLista_lanzaExcepcion() {
			OrdenLiquidable repetida = orden(40000);

			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, List.of(repetida, repetida), TASA, "liq-prov-005", AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class)
					.hasMessageContaining("repetida");
		}

		@Test
		void conDatosObligatoriosNulosOVacios_lanzaExcepcion() {
			List<OrdenLiquidable> ordenes = List.of(orden(1000));

			assertThatThrownBy(() -> Liquidacion.registrar(
					null, ordenes, TASA, "x", AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, null, "x", AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, TASA, " ", AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, TASA, "x", null))
					.isInstanceOf(LiquidacionInvalidaException.class);
		}
	}

	@Nested
	class Reconstitucion {

		@Test
		void reconstituir_restauraTalCual() {
			Liquidacion original = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-006", AHORA);

			Liquidacion reconstituida = Liquidacion.reconstituir(
					original.id(), original.comercioId(), original.ordenes(),
					original.montoBruto(), original.comisionPlataforma(),
					original.montoNetoComercio(), original.referenciaProveedor(),
					original.estado(), original.liquidadaEn());

			assertThat(reconstituida.id()).isEqualTo(original.id());
			assertThat(reconstituida.montoBruto()).isEqualTo(original.montoBruto());
			assertThat(reconstituida.estado()).isEqualTo(EstadoLiquidacion.REGISTRADA);
		}

		@Test
		void reconstituir_conCualquierDatoObligatorioNuloOVacio_lanzaExcepcion() {
			Liquidacion c = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-n", AHORA);

			assertThatThrownBy(() -> Liquidacion.reconstituir(null, c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.estado(), c.liquidadaEn()))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), null, c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.estado(), c.liquidadaEn()))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), List.of(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.estado(), c.liquidadaEn()))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), null, c.liquidadaEn()))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.estado(), null))
					.isInstanceOf(LiquidacionInvalidaException.class);
		}

		@Test
		void reconstituir_conMontosQueNoCuadran_lanzaExcepcion() {
			// defensa contra corrupción: bruto ≠ comisión + neto jamás entra
			Liquidacion original = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-007", AHORA);

			assertThatThrownBy(() -> Liquidacion.reconstituir(
					original.id(), original.comercioId(), original.ordenes(),
					Dinero.cop(40000), Dinero.cop(1000), Dinero.cop(38999),
					original.referenciaProveedor(), original.estado(), original.liquidadaEn()))
					.isInstanceOf(LiquidacionInvalidaException.class)
					.hasMessageContaining("no cuadran");
		}
	}

	@Nested
	class Identidad {

		@Test
		void dosLiquidaciones_conElMismoId_sonLaMisma() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-008", AHORA);
			Liquidacion misma = Liquidacion.reconstituir(
					liquidacion.id(), liquidacion.comercioId(), liquidacion.ordenes(),
					liquidacion.montoBruto(), liquidacion.comisionPlataforma(),
					liquidacion.montoNetoComercio(), liquidacion.referenciaProveedor(),
					liquidacion.estado(), liquidacion.liquidadaEn());
			Liquidacion otra = Liquidacion.registrar(COMERCIO,
					List.of(orden(50000)), TASA, "liq-prov-009", AHORA);

			assertThat(liquidacion).isEqualTo(misma).hasSameHashCodeAs(misma);
			assertThat(liquidacion).isNotEqualTo(otra);
			assertThat(liquidacion.hashCode()).isNotEqualTo(otra.hashCode());
			assertThat(liquidacion).isNotEqualTo("otra cosa");
		}
	}

	private static OrdenLiquidable orden(long monto) {
		return new OrdenLiquidable(IdOrden.generar(), Dinero.cop(monto));
	}

}
