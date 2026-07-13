package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.ConsultaDeVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.PaginaDeVentas;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarVentasUseCase.ResumenDeVentas;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard del comercio (HU-018). Solo rol COMERCIO: el comercio sale del
 * token — el aislamiento es estructural.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentasController {

	private final ConsultarVentasUseCase consultarVentas;

	public VentasController(ConsultarVentasUseCase consultarVentas) {
		this.consultarVentas = consultarVentas;
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
