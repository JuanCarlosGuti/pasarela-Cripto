package com.pasarela.compartido.dominio.puerto;

import com.pasarela.compartido.dominio.modelo.IdComercio;

/**
 * Puerto del kernel compartido para crear cuentas de acceso desde otros
 * contextos sin acoplarse a `seguridad`: `comercios` lo invoca al registrar
 * un comercio y `seguridad` lo implementa (los contextos no se importan
 * entre sí — ArchUnit).
 */
public interface CuentasDeAccesoPort {

	void crearCuentaDeComercio(String email, String contrasena, IdComercio comercioId);

}
