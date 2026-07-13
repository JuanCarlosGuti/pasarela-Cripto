package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.modelo.Comision;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ExportarVentasUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Exporta el historial de movimientos del comercio (HU-019). La tasa de
 * comisión es la misma que usa el registro de liquidaciones (única tasa
 * "oficial" de la plataforma hoy): cada fila muestra lo que produciría
 * liquidar esa orden sola, con la tasa vigente.
 */
@Service
public class ExportarVentasService implements ExportarVentasUseCase {

	private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

	private final OrdenDePagoRepositorio repositorio;
	private final Clock reloj;
	private final Comision comision;

	public ExportarVentasService(OrdenDePagoRepositorio repositorio, Clock reloj,
			@Value("${pasarela.liquidaciones.tasa-comision:2.5}") String tasaComision) {
		this.repositorio = repositorio;
		this.reloj = reloj;
		this.comision = new Comision(com.pasarela.compartido.dominio.modelo.Porcentaje.de(tasaComision));
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.List<FilaMovimiento> exportar(ComandoExportarVentas comando) {
		LocalDate hoy = LocalDate.ofInstant(reloj.instant(), ZONA_COLOMBIA);
		LocalDate desde = comando.desde() != null ? comando.desde() : hoy.withDayOfMonth(1);
		LocalDate hasta = comando.hasta() != null ? comando.hasta() : hoy;
		if (desde.isAfter(hasta)) {
			throw new OrdenInvalidaException(
					"El rango de fechas es inválido: 'desde' es posterior a 'hasta'");
		}
		return repositorio.listarTodasDelComercio(IdComercio.de(comando.comercioId()),
						inicioDe(desde), inicioDe(hasta.plusDays(1))) // hasta inclusivo
				.stream()
				.map(this::aFila)
				.toList();
	}

	private FilaMovimiento aFila(OrdenDePago orden) {
		return new FilaMovimiento(orden.creadaEn(), orden.referencia().valor(),
				orden.monto(), comision.calcular(orden.monto()),
				comision.netoParaElComercio(orden.monto()), orden.estado().name());
	}

	private static Instant inicioDe(LocalDate fecha) {
		return fecha.atStartOfDay(ZONA_COLOMBIA).toInstant();
	}

}
