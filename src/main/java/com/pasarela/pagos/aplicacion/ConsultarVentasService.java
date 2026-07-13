package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio.PaginaDeOrdenes;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio.VentasTotalizadas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * Dashboard del comercio (HU-018). La "venta efectiva" está definida en
 * {@link #VENTA_EFECTIVA}: el pago ya fue detectado o posterior — las
 * pendientes, expiradas, fallidas y en revisión no suman. Día y mes son
 * calendario en zona de Colombia.
 */
@Service
public class ConsultarVentasService implements ConsultarVentasUseCase {

	/** Qué cuenta como venta (la definición documentada del endpoint). */
	static final Set<EstadoOrden> VENTA_EFECTIVA = Set.of(
			EstadoOrden.PAGO_DETECTADO, EstadoOrden.CONVERTIDA, EstadoOrden.LIQUIDADA);

	private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");
	private static final int TAMANO_MAXIMO_DE_PAGINA = 100;

	private final OrdenDePagoRepositorio repositorio;
	private final Clock reloj;

	public ConsultarVentasService(OrdenDePagoRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	@Override
	@Transactional(readOnly = true)
	public ResumenDeVentas resumen(UUID comercioId) {
		IdComercio comercio = IdComercio.de(comercioId);
		LocalDate hoy = LocalDate.ofInstant(reloj.instant(), ZONA_COLOMBIA);
		YearMonth mes = YearMonth.from(hoy);

		VentasTotalizadas dia = repositorio.totalizarVentas(comercio,
				inicioDe(hoy), inicioDe(hoy.plusDays(1)), VENTA_EFECTIVA);
		VentasTotalizadas delMes = repositorio.totalizarVentas(comercio,
				inicioDe(mes.atDay(1)), inicioDe(mes.plusMonths(1).atDay(1)), VENTA_EFECTIVA);
		return new ResumenDeVentas(
				new TotalDeVentas(dia.total(), dia.cantidad()),
				new TotalDeVentas(delMes.total(), delMes.cantidad()));
	}

	@Override
	@Transactional(readOnly = true)
	public PaginaDeVentas listar(ConsultaDeVentas consulta) {
		if (consulta.pagina() < 0) {
			throw new OrdenInvalidaException("La página no puede ser negativa");
		}
		if (consulta.tamano() < 1 || consulta.tamano() > TAMANO_MAXIMO_DE_PAGINA) {
			throw new OrdenInvalidaException(
					"El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO_DE_PAGINA);
		}
		LocalDate hoy = LocalDate.ofInstant(reloj.instant(), ZONA_COLOMBIA);
		YearMonth mesActual = YearMonth.from(hoy);
		LocalDate desde = consulta.desde() != null ? consulta.desde() : mesActual.atDay(1);
		LocalDate hasta = consulta.hasta() != null
				? consulta.hasta()
				: mesActual.plusMonths(1).atDay(1).minusDays(1);
		if (desde.isAfter(hasta)) {
			throw new OrdenInvalidaException(
					"El rango de fechas es inválido: 'desde' es posterior a 'hasta'");
		}
		PaginaDeOrdenes pagina = repositorio.listarDelComercio(
				IdComercio.de(consulta.comercioId()),
				inicioDe(desde), inicioDe(hasta.plusDays(1)), // hasta inclusivo
				consulta.pagina(), consulta.tamano());
		return new PaginaDeVentas(pagina.ordenes(), pagina.totalElementos(),
				consulta.pagina(), consulta.tamano());
	}

	private static Instant inicioDe(LocalDate fecha) {
		return fecha.atStartOfDay(ZONA_COLOMBIA).toInstant();
	}

}
