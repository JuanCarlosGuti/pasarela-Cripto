package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.ConsultaDeVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.ResumenDeVentas;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio.PaginaDeOrdenes;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio.VentasTotalizadas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarVentasServiceTest {

	// 2026-07-10T15:00Z = 10:00 en Bogotá: el "hoy" del negocio es 2026-07-10
	private static final Instant AHORA = Instant.parse("2026-07-10T15:00:00Z");
	private static final UUID COMERCIO = UUID.randomUUID();

	@Mock
	private OrdenDePagoRepositorio repositorio;

	private ConsultarVentasService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ConsultarVentasService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void elResumen_cortaDiaYMesCalendarioEnZonaBogota() {
		when(repositorio.totalizarVentas(any(), any(), any(), any()))
				.thenReturn(new VentasTotalizadas(Dinero.cop(80000), 2))
				.thenReturn(new VentasTotalizadas(Dinero.cop(500000), 14));

		ResumenDeVentas resumen = servicio.resumen(COMERCIO);

		// día de Bogotá: [2026-07-10T05:00Z, 2026-07-11T05:00Z)
		verify(repositorio).totalizarVentas(eq(IdComercio.de(COMERCIO)),
				eq(Instant.parse("2026-07-10T05:00:00Z")),
				eq(Instant.parse("2026-07-11T05:00:00Z")), any());
		// mes de Bogotá: [2026-07-01T05:00Z, 2026-08-01T05:00Z)
		verify(repositorio).totalizarVentas(eq(IdComercio.de(COMERCIO)),
				eq(Instant.parse("2026-07-01T05:00:00Z")),
				eq(Instant.parse("2026-08-01T05:00:00Z")), any());
		assertThat(resumen.dia().total()).isEqualTo(Dinero.cop(80000));
		assertThat(resumen.dia().cantidad()).isEqualTo(2);
		assertThat(resumen.mes().total()).isEqualTo(Dinero.cop(500000));
	}

	@Test
	void elResumen_soloCuentaVentasEfectivas() {
		when(repositorio.totalizarVentas(any(), any(), any(), any()))
				.thenReturn(new VentasTotalizadas(Dinero.cop(0), 0));

		servicio.resumen(COMERCIO);

		// la definición del negocio: pagada o posterior — nada más suma
		verify(repositorio, org.mockito.Mockito.times(2)).totalizarVentas(any(), any(), any(),
				eq(Set.of(EstadoOrden.PAGO_DETECTADO, EstadoOrden.CONVERTIDA,
						EstadoOrden.LIQUIDADA)));
	}

	@Test
	void elListado_traduceLasFechasDeBogotaAInstantes_yPagina() {
		when(repositorio.listarDelComercio(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(new PaginaDeOrdenes(List.of(), 0));

		servicio.listar(new ConsultaDeVentas(COMERCIO,
				LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 0, 20));

		// hasta INCLUSIVO: el 10 de julio completo → límite exclusivo el 11 a las 00:00 Bogotá
		verify(repositorio).listarDelComercio(eq(IdComercio.de(COMERCIO)),
				eq(Instant.parse("2026-07-01T05:00:00Z")),
				eq(Instant.parse("2026-07-11T05:00:00Z")), eq(0), eq(20));
	}

	@Test
	void elListado_rechazaRangosYPaginacionInvalidos() {
		assertThatThrownBy(() -> servicio.listar(new ConsultaDeVentas(COMERCIO,
				LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-01"), 0, 20)))
				.isInstanceOf(OrdenInvalidaException.class)
				.hasMessageContaining("rango");
		assertThatThrownBy(() -> servicio.listar(new ConsultaDeVentas(COMERCIO,
				LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), -1, 20)))
				.isInstanceOf(OrdenInvalidaException.class);
		assertThatThrownBy(() -> servicio.listar(new ConsultaDeVentas(COMERCIO,
				LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 0, 0)))
				.isInstanceOf(OrdenInvalidaException.class);
		assertThatThrownBy(() -> servicio.listar(new ConsultaDeVentas(COMERCIO,
				LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 0, 101)))
				.isInstanceOf(OrdenInvalidaException.class)
				.hasMessageContaining("100");
	}

	@Test
	void elListado_sinFechas_usaElMesEnCurso() {
		when(repositorio.listarDelComercio(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(new PaginaDeOrdenes(List.of(), 0));

		servicio.listar(new ConsultaDeVentas(COMERCIO, null, null, 0, 20));

		verify(repositorio).listarDelComercio(eq(IdComercio.de(COMERCIO)),
				eq(Instant.parse("2026-07-01T05:00:00Z")),
				eq(Instant.parse("2026-08-01T05:00:00Z")), eq(0), eq(20));
	}

}
