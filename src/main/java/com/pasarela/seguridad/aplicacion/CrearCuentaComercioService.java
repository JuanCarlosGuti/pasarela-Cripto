package com.pasarela.seguridad.aplicacion;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.CuentasDeAccesoPort;
import com.pasarela.seguridad.dominio.excepcion.UsuarioInvalidoException;
import com.pasarela.seguridad.dominio.excepcion.UsuarioYaExisteException;
import com.pasarela.seguridad.dominio.modelo.Usuario;
import com.pasarela.seguridad.dominio.puerto.salida.HasheadorDeContrasena;
import com.pasarela.seguridad.dominio.puerto.salida.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

/**
 * Implementa el puerto compartido {@link CuentasDeAccesoPort}: crea la
 * cuenta del dueño cuando `comercios` registra un comercio, sin que ese
 * contexto conozca a `seguridad`.
 */
@Service
public class CrearCuentaComercioService implements CuentasDeAccesoPort {

	private static final int MINIMO_CARACTERES_CONTRASENA = 8;

	private final UsuarioRepositorio repositorio;
	private final HasheadorDeContrasena hasheador;
	private final Clock reloj;

	public CrearCuentaComercioService(UsuarioRepositorio repositorio,
			HasheadorDeContrasena hasheador, Clock reloj) {
		this.repositorio = repositorio;
		this.hasheador = hasheador;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public void crearCuentaDeComercio(String email, String contrasena, IdComercio comercioId) {
		if (contrasena == null || contrasena.length() < MINIMO_CARACTERES_CONTRASENA) {
			throw new UsuarioInvalidoException(
					"La contraseña debe tener al menos %d caracteres"
							.formatted(MINIMO_CARACTERES_CONTRASENA));
		}
		String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		if (repositorio.buscarPorEmail(emailNormalizado).isPresent()) {
			throw new UsuarioYaExisteException(
					"Ya existe una cuenta con el email indicado");
		}
		repositorio.guardar(Usuario.crearComercio(
				email, hasheador.hashear(contrasena), comercioId, reloj.instant()));
	}

}
