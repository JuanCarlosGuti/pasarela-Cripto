package com.pasarela.pagos.infraestructura.entrada.rest;

import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.ComandoCrearOrden;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase.OrdenCreada;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

	public OrdenController(CrearOrdenUseCase crearOrden) {
		this.crearOrden = crearOrden;
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

}
