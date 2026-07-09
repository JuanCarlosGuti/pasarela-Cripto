package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComercioUseCase.ComandoConsultarComercio;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.DecidirVerificacionUseCase.ComandoDecisionVerificacion;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase.ComandoRegistrarComercio;
import jakarta.validation.Valid;
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
@RequestMapping("/api/comercios")
public class ComercioController {

	private final RegistrarComercioUseCase registrarComercio;
	private final DecidirVerificacionUseCase decidirVerificacion;
	private final ConsultarComercioUseCase consultarComercio;

	public ComercioController(RegistrarComercioUseCase registrarComercio,
			DecidirVerificacionUseCase decidirVerificacion,
			ConsultarComercioUseCase consultarComercio) {
		this.registrarComercio = registrarComercio;
		this.decidirVerificacion = decidirVerificacion;
		this.consultarComercio = consultarComercio;
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
				solicitud.cuentaLiquidacion().titular(),
				solicitud.credenciales().email(),
				solicitud.credenciales().contrasena()));
		return ResponseEntity
				.created(uri.path("/api/comercios/{id}")
						.buildAndExpand(comercio.id().valor()).toUri())
				.body(ComercioResponse.de(comercio));
	}

	/**
	 * Consulta con aislamiento (HU-006): el ADMIN ve cualquiera; un COMERCIO
	 * solo el suyo — pedir otro responde 404, igual que uno inexistente.
	 */
	@GetMapping("/{id}")
	public ComercioResponse consultar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		String comercioIdDelToken = jwt.getClaimAsString("comercioId");
		UUID comercioDelSolicitante = "ADMIN".equals(jwt.getClaimAsString("rol"))
				? null
				: UUID.fromString(comercioIdDelToken);
		return ComercioResponse.de(consultarComercio.consultar(
				new ComandoConsultarComercio(id, comercioDelSolicitante)));
	}

	/** Decisión del Admin sobre la verificación (solo rol ADMIN, ver ConfiguracionDeSeguridadHttp). */
	@PostMapping("/{id}/verificacion")
	public ComercioResponse decidirVerificacion(
			@PathVariable UUID id,
			@Valid @RequestBody DecisionVerificacionRequest solicitud) {
		Comercio comercio = decidirVerificacion.decidir(new ComandoDecisionVerificacion(
				id, solicitud.decision(), solicitud.motivo()));
		return ComercioResponse.de(comercio);
	}

}