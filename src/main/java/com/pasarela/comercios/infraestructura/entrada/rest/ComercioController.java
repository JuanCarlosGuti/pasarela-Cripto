package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.entrada.ActualizarLimitesUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.ActualizarLimitesUseCase.ComandoActualizarLimites;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComercioUseCase.ComandoConsultarComercio;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComerciosUseCase;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comercios")
public class ComercioController {

	private final RegistrarComercioUseCase registrarComercio;
	private final DecidirVerificacionUseCase decidirVerificacion;
	private final ConsultarComercioUseCase consultarComercio;
	private final ConsultarComerciosUseCase consultarComercios;
	private final ActualizarLimitesUseCase actualizarLimites;

	public ComercioController(RegistrarComercioUseCase registrarComercio,
			DecidirVerificacionUseCase decidirVerificacion,
			ConsultarComercioUseCase consultarComercio,
			ConsultarComerciosUseCase consultarComercios,
			ActualizarLimitesUseCase actualizarLimites) {
		this.registrarComercio = registrarComercio;
		this.decidirVerificacion = decidirVerificacion;
		this.consultarComercio = consultarComercio;
		this.consultarComercios = consultarComercios;
		this.actualizarLimites = actualizarLimites;
	}

	@PostMapping
	public ResponseEntity<ComercioResponse> registrar(
			@Valid @RequestBody RegistroComercioRequest solicitud,
			UriComponentsBuilder uri) {
		Comercio comercio = registrarComercio.registrar(new ComandoRegistrarComercio(
				solicitud.razonSocial(),
				solicitud.nit(),
				solicitud.cuentaLiquidacion().banco(),
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
	 * Cola de verificación del Admin (HU-026, paginada desde HU-027): todos
	 * los comercios o solo los de un estado (?estado=PENDIENTE). Solo rol
	 * ADMIN (ver ConfiguracionDeSeguridadHttp); estado inexistente → 400.
	 */
	@GetMapping
	public PaginaComerciosResponse listar(
			@RequestParam(required = false) String estado,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		var encontrados = consultarComercios.listar(estado, pagina, tamano);
		return new PaginaComerciosResponse(
				encontrados.comercios().stream().map(ComercioResponse::de).toList(),
				encontrados.totalElementos(), encontrados.pagina(), encontrados.tamano());
	}

	public record PaginaComerciosResponse(
			List<ComercioResponse> comercios, long totalElementos, int pagina, int tamano) {
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

	/** Ajuste de topes por el Admin (HU-007); auditado en la bitácora. */
	@PutMapping("/{id}/limites")
	public ComercioResponse actualizarLimites(
			@PathVariable UUID id,
			@Valid @RequestBody ActualizacionLimitesRequest solicitud,
			@AuthenticationPrincipal Jwt jwt) {
		return ComercioResponse.de(actualizarLimites.actualizar(new ComandoActualizarLimites(
				id, solicitud.topePorTransaccion(), solicitud.topeMensual(),
				jwt.getSubject())));
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