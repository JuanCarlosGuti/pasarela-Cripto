package com.pasarela.compartido.dominio.puerto;

import java.time.Instant;

/**
 * Puerto del kernel compartido: bitácora de operaciones inusuales y cambios
 * sensibles (requisito de cumplimiento del MVP, CLAUDE.md §6). La usan
 * varios contextos (límites en comercios, webhooks anómalos en pagos) sin
 * acoplarse entre sí. El detalle jamás incluye datos sensibles.
 */
public interface BitacoraDeOperaciones {

	void registrar(RegistroDeOperacion registro);

	record RegistroDeOperacion(String tipo, String actor, String detalle, Instant momento) {
	}

}
