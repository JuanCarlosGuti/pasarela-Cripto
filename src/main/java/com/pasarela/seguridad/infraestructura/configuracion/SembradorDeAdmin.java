package com.pasarela.seguridad.infraestructura.configuracion;

import com.pasarela.compartido.infraestructura.seguridad.PropiedadesSeguridad;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.salida.HasheadorDeContrasena;
import com.pasarela.seguridad.dominio.puerto.salida.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Locale;

/**
 * Siembra la cuenta ADMIN al arrancar si no existe, con credenciales de
 * variables de entorno. Nunca un hash en una migración versionada (gitleaks)
 * y nunca la contraseña en los logs.
 */
@Component
public class SembradorDeAdmin implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SembradorDeAdmin.class);

	private final PropiedadesSeguridad propiedades;
	private final UsuarioRepositorio repositorio;
	private final HasheadorDeContrasena hasheador;
	private final Clock reloj;

	public SembradorDeAdmin(PropiedadesSeguridad propiedades, UsuarioRepositorio repositorio,
			HasheadorDeContrasena hasheador, Clock reloj) {
		this.propiedades = propiedades;
		this.repositorio = repositorio;
		this.hasheador = hasheador;
		this.reloj = reloj;
	}

	@Override
	public void run(ApplicationArguments argumentos) {
		PropiedadesSeguridad.Admin admin = propiedades.admin();
		if (admin == null || admin.email() == null || admin.email().isBlank()
				|| admin.contrasena() == null || admin.contrasena().isBlank()) {
			log.warn("Sin credenciales de admin configuradas: no se siembra la cuenta ADMIN");
			return;
		}
		String email = admin.email().trim().toLowerCase(Locale.ROOT);
		if (repositorio.buscarPorEmail(email).isPresent()) {
			return;
		}
		repositorio.guardar(Usuario.crearAdmin(
				email, hasheador.hashear(admin.contrasena()), reloj.instant()));
		log.info("Cuenta ADMIN sembrada para {}", email);
	}

}
