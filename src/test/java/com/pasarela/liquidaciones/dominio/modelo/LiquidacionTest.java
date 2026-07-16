package com.pasarela.liquidaciones.dominio.modelo;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.compartido.dominio.puerto.LiquidadorDeOrdenes.OrdenLiquidable;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiquidacionTest {

	private static final Instant AHORA = Instant.parse("2026-07-10T18:00:00Z");
	private static final Porcentaje TASA = Porcentaje.de("2.5");
	private static final IdComercio COMERCIO = IdComercio.generar();
	// comisión de rampa en 0 por defecto: no interfiere con las cuentas ya
	// existentes de bruto/comisión/neto salvo en el test dedicado de HU-025
	private static final DetalleRampa DETALLE = new DetalleRampa(
			Dinero.cop(0), new BigDecimal("4150"), "NEQUI ••••4567 — Comercio");

	@Nested
	class Registro {

		@Test
		void agrupaLasOrdenes_yCalculaBrutoComisionYNeto() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO, List.of(
							orden(40000), orden(60000)),
					TASA, "liq-prov-001", DETALLE, AHORA);

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
					TASA, "liq-prov-002", DETALLE, AHORA);

			assertThat(liquidacion.montoBruto()).isEqualTo(Dinero.cop(99999));
			assertThat(liquidacion.comisionPlataforma()).isEqualTo(Dinero.cop(2500));
			assertThat(liquidacion.montoNetoComercio()).isEqualTo(Dinero.cop(97499));
			// la propiedad inviolable: comisión + neto = bruto, SIEMPRE
			assertThat(liquidacion.comisionPlataforma()
					.sumar(liquidacion.montoNetoComercio()))
					.isEqualTo(liquidacion.montoBruto());
		}

		@Test
		void conComisionDeRampa_elNetoLaDescuentaTambien_yTodoSigueCuadrando() {
			// HU-025: la rampa también se lleva su tajada, antes que el neto
			DetalleRampa conComision = new DetalleRampa(
					Dinero.cop(800), new BigDecimal("4150"), "NEQUI ••••4567 — Comercio");

			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO, List.of(orden(100000)),
					TASA, "liq-prov-rampa", conComision, AHORA);

			assertThat(liquidacion.montoBruto()).isEqualTo(Dinero.cop(100000));
			assertThat(liquidacion.comisionPlataforma()).isEqualTo(Dinero.cop(2500));
			assertThat(liquidacion.detalleRampa().comisionRampa()).isEqualTo(Dinero.cop(800));
			assertThat(liquidacion.montoNetoComercio()).isEqualTo(Dinero.cop(96700));
			assertThat(liquidacion.comisionPlataforma()
					.sumar(liquidacion.detalleRampa().comisionRampa())
					.sumar(liquidacion.montoNetoComercio()))
					.isEqualTo(liquidacion.montoBruto());
		}

		@Test
		void brutoEsExactamenteLaSumaDeLasOrdenes() {
			List<OrdenLiquidable> ordenes = List.of(
					orden(17), orden(23), orden(41), orden(999983));

			Liquidacion liquidacion = Liquidacion.registrar(
					COMERCIO, ordenes, TASA, "liq-prov-003", DETALLE, AHORA);

			assertThat(liquidacion.montoBruto()).isEqualTo(Dinero.cop(17 + 23 + 41 + 999983));
		}

		@Test
		void sinOrdenes_lanzaExcepcion() {
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, List.of(), TASA, "liq-prov-004", DETALLE, AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class)
					.hasMessageContaining("al menos una orden");
		}

		@Test
		void conOrdenesRepetidasEnLaLista_lanzaExcepcion() {
			OrdenLiquidable repetida = orden(40000);

			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, List.of(repetida, repetida), TASA, "liq-prov-005", DETALLE, AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class)
					.hasMessageContaining("repetida");
		}

		@Test
		void conDatosObligatoriosNulosOVacios_lanzaExcepcion() {
			List<OrdenLiquidable> ordenes = List.of(orden(1000));

			assertThatThrownBy(() -> Liquidacion.registrar(
					null, ordenes, TASA, "x", DETALLE, AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, null, "x", DETALLE, AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, TASA, " ", DETALLE, AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, TASA, "x", null, AHORA))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.registrar(
					COMERCIO, ordenes, TASA, "x", DETALLE, null))
					.isInstanceOf(LiquidacionInvalidaException.class);
		}
	}

	@Nested
	class Conciliacion {

		@Test
		void siLosDatosDelProveedorCoinciden_pasaAConciliada() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000), orden(60000)), TASA, "liq-c-1", DETALLE, AHORA);

			liquidacion.conciliar(new ReporteDelProveedor(
					Dinero.cop(100000), liquidacion.ordenes()));

			assertThat(liquidacion.estado()).isEqualTo(EstadoLiquidacion.CONCILIADA);
			assertThat(liquidacion.detalleDiscrepancia()).isNull();
		}

		@Test
		void unMontoDistinto_marcaDiscrepancia_conAmbosMontosEnElDetalle() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-c-2", DETALLE, AHORA);

			liquidacion.conciliar(new ReporteDelProveedor(
					Dinero.cop(39000), liquidacion.ordenes()));

			assertThat(liquidacion.estado()).isEqualTo(EstadoLiquidacion.DISCREPANCIA);
			assertThat(liquidacion.detalleDiscrepancia())
					.contains("40000").contains("39000");
		}

		@Test
		void ordenesFaltantesOSobrantes_marcanDiscrepancia_conLosIdsEnElDetalle() {
			OrdenLiquidable registrada = orden(40000);
			IdOrden sobrante = IdOrden.generar();
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(registrada), TASA, "liq-c-3", DETALLE, AHORA);

			// el proveedor reporta una orden distinta: la nuestra falta, la suya sobra
			liquidacion.conciliar(new ReporteDelProveedor(
					Dinero.cop(40000), List.of(sobrante)));

			assertThat(liquidacion.estado()).isEqualTo(EstadoLiquidacion.DISCREPANCIA);
			assertThat(liquidacion.detalleDiscrepancia())
					.contains(registrada.id().valor().toString())  // faltante
					.contains(sobrante.valor().toString());        // sobrante
		}

		@Test
		void unaLiquidacionYaConciliada_noSeReconcilia() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-c-4", DETALLE, AHORA);
			ReporteDelProveedor reporte = new ReporteDelProveedor(
					Dinero.cop(40000), liquidacion.ordenes());
			liquidacion.conciliar(reporte);

			assertThatThrownBy(() -> liquidacion.conciliar(reporte))
					.isInstanceOf(com.pasarela.liquidaciones.dominio.excepcion.ConciliacionInvalidaException.class);
			assertThat(liquidacion.estado()).isEqualTo(EstadoLiquidacion.CONCILIADA);
		}

		@Test
		void conciliar_conReporteNulo_lanzaExcepcion() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-c-5", DETALLE, AHORA);

			assertThatThrownBy(() -> liquidacion.conciliar(null))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThat(liquidacion.estado()).isEqualTo(EstadoLiquidacion.REGISTRADA);
		}
	}

	@Nested
	class Reconstitucion {

		@Test
		void reconstituir_restauraTalCual() {
			Liquidacion original = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-006", DETALLE, AHORA);

			Liquidacion reconstituida = Liquidacion.reconstituir(
					original.id(), original.comercioId(), original.ordenes(),
					original.montoBruto(), original.comisionPlataforma(),
					original.montoNetoComercio(), original.referenciaProveedor(),
					original.detalleRampa(), original.estado(), original.liquidadaEn(), null);

			assertThat(reconstituida.id()).isEqualTo(original.id());
			assertThat(reconstituida.montoBruto()).isEqualTo(original.montoBruto());
			assertThat(reconstituida.estado()).isEqualTo(EstadoLiquidacion.REGISTRADA);
		}

		@Test
		void reconstituir_conCualquierDatoObligatorioNuloOVacio_lanzaExcepcion() {
			Liquidacion c = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-n", DETALLE, AHORA);

			assertThatThrownBy(() -> Liquidacion.reconstituir(null, c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.detalleRampa(), c.estado(), c.liquidadaEn(), null))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), null, c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.detalleRampa(), c.estado(), c.liquidadaEn(), null))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), List.of(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.detalleRampa(), c.estado(), c.liquidadaEn(), null))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), null, c.estado(), c.liquidadaEn(), null))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.detalleRampa(), null, c.liquidadaEn(), null))
					.isInstanceOf(LiquidacionInvalidaException.class);
			assertThatThrownBy(() -> Liquidacion.reconstituir(c.id(), c.comercioId(), c.ordenes(),
					c.montoBruto(), c.comisionPlataforma(), c.montoNetoComercio(),
					c.referenciaProveedor(), c.detalleRampa(), c.estado(), null, null))
					.isInstanceOf(LiquidacionInvalidaException.class);
		}

		@Test
		void reconstituir_conMontosQueNoCuadran_lanzaExcepcion() {
			// defensa contra corrupción: bruto ≠ comisión + rampa + neto jamás entra
			Liquidacion original = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-007", DETALLE, AHORA);

			assertThatThrownBy(() -> Liquidacion.reconstituir(
					original.id(), original.comercioId(), original.ordenes(),
					Dinero.cop(40000), Dinero.cop(1000), Dinero.cop(38999),
					original.referenciaProveedor(), original.detalleRampa(), original.estado(),
					original.liquidadaEn(), null))
					.isInstanceOf(LiquidacionInvalidaException.class)
					.hasMessageContaining("no cuadran");
		}
	}

	@Nested
	class Identidad {

		@Test
		void dosLiquidaciones_conElMismoId_sonLaMisma() {
			Liquidacion liquidacion = Liquidacion.registrar(COMERCIO,
					List.of(orden(40000)), TASA, "liq-prov-008", DETALLE, AHORA);
			Liquidacion misma = Liquidacion.reconstituir(
					liquidacion.id(), liquidacion.comercioId(), liquidacion.ordenes(),
					liquidacion.montoBruto(), liquidacion.comisionPlataforma(),
					liquidacion.montoNetoComercio(), liquidacion.referenciaProveedor(),
					liquidacion.detalleRampa(), liquidacion.estado(), liquidacion.liquidadaEn(),
					null);
			Liquidacion otra = Liquidacion.registrar(COMERCIO,
					List.of(orden(50000)), TASA, "liq-prov-009", DETALLE, AHORA);

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
