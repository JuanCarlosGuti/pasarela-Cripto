package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ExportarVentasUseCase.ComandoExportarVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ExportarVentasUseCase.FilaMovimiento;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportarVentasServiceTest {

	// 2026-07-10T15:00Z = 10:00 en Bogotá
	private static final Instant AHORA = Instant.parse("2026-07-10T15:00:00Z");
	private static final UUID COMERCIO = UUID.randomUUID();

	@Mock
	private OrdenDePagoRepositorio repositorio;

	private ExportarVentasService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ExportarVentasService(
				repositorio, Clock.fixed(AHORA, ZoneOffset.UTC), "2.5");
	}

	@Test
	void cadaFila_traeBrutoComisionYNeto_cuadrandoAlCentavo() {
		OrdenDePago orden = ordenDe(40000, EstadoDePrueba.PAGO_DETECTADO);
		when(repositorio.listarTodasDelComercio(any(), any(), any()))
				.thenReturn(List.of(orden));

		List<FilaMovimiento> filas = servicio.exportar(new ComandoExportarVentas(
				COMERCIO, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10")));

		assertThat(filas).hasSize(1);
		FilaMovimiento fila = filas.get(0);
		assertThat(fila.referencia()).isEqualTo(orden.referencia().valor());
		assertThat(fila.montoBruto()).isEqualTo(Dinero.cop(40000));
		assertThat(fila.comision()).isEqualTo(Dinero.cop(1000)); // 2.5% de 40000
		assertThat(fila.neto()).isEqualTo(Dinero.cop(39000));
		assertThat(fila.comision().sumar(fila.neto())).isEqualTo(fila.montoBruto());
		assertThat(fila.estado()).isEqualTo("PAGO_DETECTADO");
		assertThat(fila.fecha()).isEqualTo(orden.creadaEn());
	}

	@Test
	void incluyeOrdenesEnCualquierEstado_esElHistorialCompleto() {
		OrdenDePago pendiente = ordenDe(10000, EstadoDePrueba.PENDIENTE_PAGO);
		OrdenDePago expirada = ordenDe(20000, EstadoDePrueba.EXPIRADA);
		when(repositorio.listarTodasDelComercio(any(), any(), any()))
				.thenReturn(List.of(pendiente, expirada));

		List<FilaMovimiento> filas = servicio.exportar(new ComandoExportarVentas(
				COMERCIO, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10")));

		assertThat(filas).extracting(FilaMovimiento::estado)
				.containsExactlyInAnyOrder("PENDIENTE_PAGO", "EXPIRADA");
	}

	@Test
	void unRangoSinMovimientos_devuelveListaVacia() {
		when(repositorio.listarTodasDelComercio(any(), any(), any())).thenReturn(List.of());

		List<FilaMovimiento> filas = servicio.exportar(new ComandoExportarVentas(
				COMERCIO, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10")));

		assertThat(filas).isEmpty();
	}

	@Test
	void unRangoInvalido_lanzaExcepcion_sinConsultarElRepositorio() {
		assertThatThrownBy(() -> servicio.exportar(new ComandoExportarVentas(
				COMERCIO, LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-01"))))
				.isInstanceOf(OrdenInvalidaException.class)
				.hasMessageContaining("rango");

		verify(repositorio, org.mockito.Mockito.never()).listarTodasDelComercio(any(), any(), any());
	}

	@Test
	void traduceElRangoDeFechasDeBogotaAInstantes_hastaInclusivo() {
		when(repositorio.listarTodasDelComercio(any(), any(), any())).thenReturn(List.of());

		servicio.exportar(new ComandoExportarVentas(
				COMERCIO, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10")));

		// el 10 de julio completo → límite exclusivo el 11 a las 00:00 Bogotá
		verify(repositorio).listarTodasDelComercio(eq(IdComercio.de(COMERCIO)),
				eq(Instant.parse("2026-07-01T05:00:00Z")),
				eq(Instant.parse("2026-07-11T05:00:00Z")));
	}

	private enum EstadoDePrueba { PENDIENTE_PAGO, PAGO_DETECTADO, EXPIRADA }

	private static OrdenDePago ordenDe(long monto, EstadoDePrueba destino) {
		OrdenDePago orden = OrdenDePago.crear(IdComercio.de(COMERCIO), Dinero.cop(monto),
				ReferenciaPago.generar(), AHORA.minus(Duration.ofDays(2)),
				AHORA.minus(Duration.ofDays(2)).plus(Duration.ofMinutes(15)));
		switch (destino) {
			case PENDIENTE_PAGO -> orden.registrarCobroEnProveedor(AHORA.minus(Duration.ofDays(2)));
			case PAGO_DETECTADO -> {
				orden.registrarCobroEnProveedor(AHORA.minus(Duration.ofDays(2)));
				orden.confirmarPago(
						new com.pasarela.pagos.dominio.modelo.EventoPago(
								orden.referencia(), orden.monto(), AHORA.minus(Duration.ofDays(2))),
						AHORA.minus(Duration.ofDays(2)).plusSeconds(5));
			}
			case EXPIRADA -> {
				orden.registrarCobroEnProveedor(AHORA.minus(Duration.ofDays(2)));
				orden.expirar(orden.expiraEn().plusSeconds(1));
			}
		}
		return orden;
	}

}
