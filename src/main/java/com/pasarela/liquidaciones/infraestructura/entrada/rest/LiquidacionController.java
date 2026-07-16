package com.pasarela.liquidaciones.infraestructura.entrada.rest;

import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import com.pasarela.liquidaciones.dominio.puerto.entrada.ConciliarLiquidacionUseCase;
import com.pasarela.liquidaciones.dominio.puerto.entrada.ConsultarLiquidacionesUseCase;
import com.pasarela.liquidaciones.dominio.puerto.entrada.RegistrarLiquidacionUseCase;
import com.pasarela.liquidaciones.dominio.puerto.entrada.RegistrarLiquidacionUseCase.ComandoRegistrarLiquidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Registro y conciliación de liquidaciones (HU-016/017, solo ADMIN) y
 * consulta de las propias liquidaciones del comercio (HU-025, solo
 * COMERCIO — el comercio sale del token).
 */
@RestController
@RequestMapping("/api/liquidaciones")
public class LiquidacionController {

	private final RegistrarLiquidacionUseCase registrarLiquidacion;
	private final ConciliarLiquidacionUseCase conciliarLiquidacion;
	private final ConsultarLiquidacionesUseCase consultarLiquidaciones;

	public LiquidacionController(RegistrarLiquidacionUseCase registrarLiquidacion,
			ConciliarLiquidacionUseCase conciliarLiquidacion,
			ConsultarLiquidacionesUseCase consultarLiquidaciones) {
		this.registrarLiquidacion = registrarLiquidacion;
		this.conciliarLiquidacion = conciliarLiquidacion;
		this.consultarLiquidaciones = consultarLiquidaciones;
	}

	@PostMapping
	public ResponseEntity<LiquidacionResponse> registrar(
			@Valid @RequestBody RegistroLiquidacionRequest solicitud,
			UriComponentsBuilder uri) {
		Liquidacion liquidacion = registrarLiquidacion.registrar(
				new ComandoRegistrarLiquidacion(solicitud.comercioId(), solicitud.ordenes()));
		return ResponseEntity
				.created(uri.path("/api/liquidaciones/{id}")
						.buildAndExpand(liquidacion.id().valor()).toUri())
				.body(LiquidacionResponse.de(liquidacion));
	}

	/** Conciliación contra lo reportado por el proveedor (HU-017). Solo ADMIN. */
	@PostMapping("/{id}/conciliacion")
	public LiquidacionResponse conciliar(
			@PathVariable UUID id,
			@Valid @RequestBody ConciliacionRequest solicitud,
			@AuthenticationPrincipal Jwt jwt) {
		return LiquidacionResponse.de(conciliarLiquidacion.conciliar(
				new ConciliarLiquidacionUseCase.ComandoConciliar(
						id, solicitud.montoBruto(), solicitud.ordenes(), jwt.getSubject())));
	}

	/** SOLO SIMULACIÓN (HU-025): la conversión la arma ProveedorDeRampaSimulado. */
	@GetMapping
	public List<LiquidacionResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		return consultarLiquidaciones.listar(comercioDe(jwt)).stream()
				.map(LiquidacionResponse::de).toList();
	}

	private static UUID comercioDe(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("comercioId"));
	}

	public record ConciliacionRequest(
			@NotNull(message = "El monto bruto reportado es obligatorio")
			Long montoBruto,
			@NotEmpty(message = "El reporte requiere las órdenes incluidas")
			List<UUID> ordenes) {
	}

	public record RegistroLiquidacionRequest(
			@NotNull(message = "El comercio es obligatorio")
			UUID comercioId,
			@NotEmpty(message = "La liquidación requiere al menos una orden")
			List<UUID> ordenes) {
	}

	public record LiquidacionResponse(
			UUID id,
			UUID comercioId,
			List<UUID> ordenes,
			long montoBruto,
			long comisionPlataforma,
			long comisionRampa,
			BigDecimal tasaCambioSimulada,
			String cuentaDestinoDescripcion,
			long montoNetoComercio,
			String referenciaProveedor,
			String estado,
			String detalleDiscrepancia,
			java.time.Instant liquidadaEn) {

		static LiquidacionResponse de(Liquidacion liquidacion) {
			return new LiquidacionResponse(
					liquidacion.id().valor(),
					liquidacion.comercioId().valor(),
					liquidacion.ordenes().stream()
							.map(com.pasarela.compartido.dominio.modelo.IdOrden::valor).toList(),
					liquidacion.montoBruto().monto().longValue(),
					liquidacion.comisionPlataforma().monto().longValue(),
					liquidacion.detalleRampa().comisionRampa().monto().longValue(),
					liquidacion.detalleRampa().tasaCambioSimulada(),
					liquidacion.detalleRampa().cuentaDestinoDescripcion(),
					liquidacion.montoNetoComercio().monto().longValue(),
					liquidacion.referenciaProveedor(),
					liquidacion.estado().name(),
					liquidacion.detalleDiscrepancia(),
					liquidacion.liquidadaEn());
		}
	}

}
