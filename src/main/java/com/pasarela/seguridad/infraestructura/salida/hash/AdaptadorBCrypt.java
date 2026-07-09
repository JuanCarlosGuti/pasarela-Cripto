package com.pasarela.seguridad.infraestructura.salida.hash;

import com.pasarela.seguridad.dominio.puerto.salida.HasheadorDeContrasena;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt como implementación del puerto de hashing (HU-006). */
@Component
public class AdaptadorBCrypt implements HasheadorDeContrasena {

	private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

	@Override
	public String hashear(String contrasenaEnClaro) {
		return codificador.encode(contrasenaEnClaro);
	}

	@Override
	public boolean coincide(String contrasenaEnClaro, String hash) {
		return codificador.matches(contrasenaEnClaro, hash);
	}

}
