package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase.ComandoConsultarOrden;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.ComandoCrearOrden;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.OrdenCreada;
import com.pasarela.pagos.dominio.puerto.entrada.GenerarComprobanteUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.GenerarComprobanteUseCase.ComandoGenerarComprobante;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final CrearOrdenUseCase crearOrden;
	private final ConsultarOrdenUseCase consultarOrden;
	private final GenerarComprobanteUseCase generarComprobante;

	public OrdenController(CrearOrdenUseCase crearOrden, ConsultarOrdenUseCase consultarOrden,
			GenerarComprobanteUseCase generarComprobante) {
		this.crearOrden = crearOrden;
		this.consultarOrden = consultarOrden;
		this.generarComprobante = generarComprobante;
	}

	/**
	 * Detalle para el dueño (HU-009): el ADMIN ve cualquiera; un COMERCIO
	 * solo las suyas — pedir otra responde 404, igual que una inexistente.
	 * Es el endpoint de polling de la caja (ADR-005): sin caché intermedio.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<OrdenDetalleResponse> consultar(
			@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(OrdenDetalleResponse.de(consultarOrden.consultar(
						new ComandoConsultarOrden(id, comercioDelSolicitante(jwt)))));
	}

	/**
	 * Comprobante de venta (HU-020): solo existe para órdenes con el pago
	 * detectado o posterior; pedirlo para una no pagada responde 422. Mismo
	 * aislamiento que el detalle: ADMIN cualquiera, COMERCIO solo las suyas.
	 */
	@Operation(summary = "Comprobante de una venta",
			description = "Soporte de la venta para el cliente y el contador: número (el id "
					+ "de la orden — pedirlo dos veces da el mismo comprobante), referencia "
					+ "del cobro ante el proveedor, monto, estado y timestamps de creación, "
					+ "pago y liquidación. Solo órdenes PAGO_DETECTADO, CONVERTIDA o "
					+ "LIQUIDADA; las demás responden 422.")
	@GetMapping("/{id}/comprobante")
	public ComprobanteResponse comprobante(
			@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return ComprobanteResponse.de(generarComprobante.generar(
				new ComandoGenerarComprobante(id, comercioDelSolicitante(jwt))));
	}

	/** Solo rol COMERCIO (ver ConfiguracionDeSeguridadHttp): el comercio sale del token. */
	@PostMapping
	public ResponseEntity<OrdenCreadaResponse> crear(
			@Valid @RequestBody CrearOrdenRequest solicitud,
			@AuthenticationPrincipal Jwt jwt,
			UriComponentsBuilder uri) {
		UUID comercioId = UUID.fromString(jwt.getClaimAsString("comercioId"));
		OrdenCreada creada = crearOrden.crear(
				new ComandoCrearOrden(comercioId, solicitud.monto()));
		return ResponseEntity
				.created(uri.path("/api/ordenes/{id}")
						.buildAndExpand(creada.orden().id().valor()).toUri())
				.body(OrdenCreadaResponse.de(creada));
	}

	/** ADMIN consulta cualquiera (null); un COMERCIO solo lo suyo (HU-009). */
	private static UUID comercioDelSolicitante(Jwt jwt) {
		return "ADMIN".equals(jwt.getClaimAsString("rol"))
				? null
				: UUID.fromString(jwt.getClaimAsString("comercioId"));
	}

}
