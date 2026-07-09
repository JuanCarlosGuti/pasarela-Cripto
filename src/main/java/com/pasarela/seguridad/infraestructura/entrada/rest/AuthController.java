package com.pasarela.seguridad.infraestructura.entrada.rest;

import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.entrada.AutenticarUsuarioUseCase;
import com.pasarela.seguridad.dominio.puerto.entrada.AutenticarUsuarioUseCase.ComandoAutenticar;
import com.pasarela.seguridad.infraestructura.token.ServicioDeTokens;
import com.pasarela.seguridad.infraestructura.token.ServicioDeTokens.TokenEmitido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AutenticarUsuarioUseCase autenticarUsuario;
	private final ServicioDeTokens tokens;

	public AuthController(AutenticarUsuarioUseCase autenticarUsuario, ServicioDeTokens tokens) {
		this.autenticarUsuario = autenticarUsuario;
		this.tokens = tokens;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest solicitud) {
		Usuario usuario = autenticarUsuario.autenticar(
				new ComandoAutenticar(solicitud.usuario(), solicitud.contrasena()));
		TokenEmitido emitido = tokens.emitir(usuario);
		return new LoginResponse(emitido.token(), emitido.expiraEn(), usuario.rol().name());
	}

	public record LoginRequest(
			@NotBlank(message = "El usuario es obligatorio")
			String usuario,
			@NotBlank(message = "La contraseña es obligatoria")
			String contrasena) {
	}

	public record LoginResponse(String token, Instant expiraEn, String rol) {
	}

}
