package com.pasarela.liquidaciones.infraestructura.entrada.rest;

import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import com.pasarela.liquidaciones.dominio.puerto.entrada.RegistrarLiquidacionUseCase;
import com.pasarela.liquidaciones.dominio.puerto.entrada.RegistrarLiquidacionUseCase.ComandoRegistrarLiquidacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/** Registro de liquidaciones (HU-016). Solo rol ADMIN (ver seguridad HTTP). */
@RestController
@RequestMapping("/api/liquidaciones")
public class LiquidacionController {

	private final RegistrarLiquidacionUseCase registrarLiquidacion;

	public LiquidacionController(RegistrarLiquidacionUseCase registrarLiquidacion) {
		this.registrarLiquidacion = registrarLiquidacion;
	}

	@PostMapping
	public ResponseEntity<LiquidacionResponse> registrar(
			@Valid @RequestBody RegistroLiquidacionRequest solicitud,
			UriComponentsBuilder uri) {
		Liquidacion liquidacion = registrarLiquidacion.registrar(
				new ComandoRegistrarLiquidacion(
						solicitud.comercioId(), solicitud.ordenes(),
						solicitud.referenciaProveedor()));
		return ResponseEntity
				.created(uri.path("/api/liquidaciones/{id}")
						.buildAndExpand(liquidacion.id().valor()).toUri())
				.body(LiquidacionResponse.de(liquidacion));
	}

	public record RegistroLiquidacionRequest(
			@NotNull(message = "El comercio es obligatorio")
			UUID comercioId,
			@NotEmpty(message = "La liquidación requiere al menos una orden")
			List<UUID> ordenes,
			@NotBlank(message = "La referencia del proveedor es obligatoria")
			String referenciaProveedor) {
	}

	public record LiquidacionResponse(
			UUID id,
			UUID comercioId,
			List<UUID> ordenes,
			long montoBruto,
			long comisionPlataforma,
			long montoNetoComercio,
			String referenciaProveedor,
			String estado) {

		static LiquidacionResponse de(Liquidacion liquidacion) {
			return new LiquidacionResponse(
					liquidacion.id().valor(),
					liquidacion.comercioId().valor(),
					liquidacion.ordenes().stream()
							.map(com.pasarela.compartido.dominio.modelo.IdOrden::valor).toList(),
					liquidacion.montoBruto().monto().longValue(),
					liquidacion.comisionPlataforma().monto().longValue(),
					liquidacion.montoNetoComercio().monto().longValue(),
					liquidacion.referenciaProveedor(),
					liquidacion.estado().name());
		}
	}

}
