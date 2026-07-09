package com.pasarela.comercios.infraestructura.entrada.rest;

import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase.ComandoRegistrarComercio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/comercios")
public class ComercioController {

	private final RegistrarComercioUseCase registrarComercio;

	public ComercioController(RegistrarComercioUseCase registrarComercio) {
		this.registrarComercio = registrarComercio;
	}

	@PostMapping
	public ResponseEntity<ComercioRegistradoResponse> registrar(
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
				.body(ComercioRegistradoResponse.de(comercio));
	}

}