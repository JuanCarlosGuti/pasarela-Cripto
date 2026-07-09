package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase.ComandoDecisionVerificacion;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase.ComandoRegistrarComercio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/comercios")
public class ComercioController {

	private final RegistrarComercioUseCase registrarComercio;
	private final DecidirVerificacionUseCase decidirVerificacion;

	public ComercioController(RegistrarComercioUseCase registrarComercio,
			DecidirVerificacionUseCase decidirVerificacion) {
		this.registrarComercio = registrarComercio;
		this.decidirVerificacion = decidirVerificacion;
	}

	@PostMapping
	public ResponseEntity<ComercioResponse> registrar(
			@Valid @RequestBody RegistroComercioRequest solicitud,
			UriComponentsBuilder uri) {
		Comercio comercio = registrarComercio.registrar(new ComandoRegistrarComercio(
				solicitud.razonSocial(),
				solicitud.nit(),
				solicitud.cuentaLiquidacion().tipo(),
				solicitud.cuentaLiquidacion().numero(),
				solicitud.cuentaLiquidacion().titular()));
		return ResponseEntity
				.created(uri.path("/api/comercios/{id}")
						.buildAndExpand(comercio.id().valor()).toUri())
				.body(ComercioResponse.de(comercio));
	}

	/** Decisión del Admin sobre la verificación (solo rol ADMIN desde HU-006). */
	@PostMapping("/{id}/verificacion")
	public ComercioResponse decidirVerificacion(
			@PathVariable UUID id,
			@Valid @RequestBody DecisionVerificacionRequest solicitud) {
		Comercio comercio = decidirVerificacion.decidir(new ComandoDecisionVerificacion(
				id, solicitud.decision(), solicitud.motivo()));
		return ComercioResponse.de(comercio);
	}

}