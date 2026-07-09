package com.pasarela.seguridad.dominio.puerto.salida;

/**
 * Puerto de salida: hashing de contraseñas. El dominio no conoce el
 * algoritmo (BCrypt vive en la infraestructura).
 */
public interface HasheadorDeContrasena {

	String hashear(String contrasenaEnClaro);

	boolean coincide(String contrasenaEnClaro, String hash);

}
