package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.ConsultaDeVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.PaginaDeVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.ResumenDeVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ExportarVentasUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ExportarVentasUseCase.ComandoExportarVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ExportarVentasUseCase.FilaMovimiento;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard del comercio (HU-018/019). Solo rol COMERCIO: el comercio sale
 * del token — el aislamiento es estructural.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentasController {

	private static final DateTimeFormatter FORMATO_FECHA_CSV =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/Bogota"));

	private final ConsultarVentasUseCase consultarVentas;
	private final ExportarVentasUseCase exportarVentas;

	public VentasController(ConsultarVentasUseCase consultarVentas,
			ExportarVentasUseCase exportarVentas) {
		this.consultarVentas = consultarVentas;
		this.exportarVentas = exportarVentas;
	}

	@Operation(summary = "Ventas del día y del mes en curso",
			description = "Definición de venta: una orden cuenta como venta si su pago ya fue "
					+ "detectado o posterior (PAGO_DETECTADO, CONVERTIDA o LIQUIDADA). Las "
					+ "pendientes, expiradas, fallidas y en revisión NO suman. Día y mes son "
					+ "calendario en zona America/Bogota.")
	@GetMapping("/resumen")
	public ResumenVentasResponse resumen(@AuthenticationPrincipal Jwt jwt) {
		ResumenDeVentas resumen = consultarVentas.resumen(comercioDe(jwt));
		return new ResumenVentasResponse(
				new TotalResponse(resumen.dia().total().monto().longValue(),
						resumen.dia().cantidad()),
				new TotalResponse(resumen.mes().total().monto().longValue(),
						resumen.mes().cantidad()));
	}

	@Operation(summary = "Historial de órdenes del comercio, paginado",
			description = "Incluye TODOS los estados (también pendientes y expiradas). Rango "
					+ "[desde, hasta] inclusivo en fechas de Colombia; sin fechas, el mes en "
					+ "curso. Tamaño de página máximo: 100.")
	@GetMapping
	public PaginaVentasResponse listar(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		PaginaDeVentas ventas = consultarVentas.listar(
				new ConsultaDeVentas(comercioDe(jwt), desde, hasta, pagina, tamano));
		return new PaginaVentasResponse(
				ventas.ordenes().stream().map(VentaResponse::de).toList(),
				ventas.totalElementos(), ventas.pagina(), ventas.tamano());
	}

	@Operation(summary = "Exportar el historial de movimientos a CSV",
			description = "Columnas: Fecha;Referencia;Monto bruto;Comisión;Neto;Estado. "
					+ "Separador ';' y BOM UTF-8 (compatible con Excel en español). Incluye "
					+ "TODOS los estados (es el historial completo, no solo ventas efectivas). "
					+ "Rango [desde, hasta] inclusivo en fechas de Colombia; sin fechas, el mes "
					+ "en curso. Un rango sin movimientos devuelve un archivo válido con solo "
					+ "encabezados.")
	@GetMapping(value = "/exportar", produces = "text/csv;charset=UTF-8")
	public ResponseEntity<byte[]> exportar(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) LocalDate desde,
			@RequestParam(required = false) LocalDate hasta) {
		List<FilaMovimiento> filas = exportarVentas.exportar(
				new ComandoExportarVentas(comercioDe(jwt), desde, hasta));
		byte[] csv = aCsv(filas);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"movimientos.csv\"")
				.body(csv);
	}

	/**
	 * BOM UTF-8 + cabecera + filas separadas por ';' (Excel en español). Sin
	 * escapado CSV: los campos (fecha formateada, UUID, número, enum) nunca
	 * contienen ';' ni saltos de línea.
	 */
	private static byte[] aCsv(List<FilaMovimiento> filas) {
		StringBuilder csv = new StringBuilder("﻿"); // BOM UTF-8
		csv.append("Fecha;Referencia;Monto bruto;Comisión;Neto;Estado\r\n");
		for (FilaMovimiento fila : filas) {
			csv.append(FORMATO_FECHA_CSV.format(fila.fecha())).append(';')
					.append(fila.referencia()).append(';')
					.append(fila.montoBruto().monto().longValue()).append(';')
					.append(fila.comision().monto().longValue()).append(';')
					.append(fila.neto().monto().longValue()).append(';')
					.append(fila.estado()).append("\r\n");
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static UUID comercioDe(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("comercioId"));
	}

	public record ResumenVentasResponse(TotalResponse dia, TotalResponse mes) {
	}

	public record TotalResponse(long total, long cantidad) {
	}

	public record PaginaVentasResponse(List<VentaResponse> ordenes, long totalElementos,
			int pagina, int tamano) {
	}

	public record VentaResponse(UUID id, String referencia, String estado, long monto,
			Instant creadaEn) {

		static VentaResponse de(OrdenDePago orden) {
			return new VentaResponse(orden.id().valor(), orden.referencia().valor(),
					orden.estado().name(), orden.monto().monto().longValue(),
					orden.creadaEn());
		}
	}

}
