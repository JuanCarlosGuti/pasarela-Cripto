package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.TestcontainersConfiguration;
import com.pasarela.pagos.dominio.modelo.EventoProveedor;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.EventoProveedorRepositorio.EventoDuplicadoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-011: la unicidad (proveedor, id_externo_evento) existe como constraint
 * en la BASE DE DATOS, no solo en Java — probada a nivel SQL y a través del
 * adaptador. Nota: el adaptador escribe con REQUIRES_NEW (commit propio),
 * así que cada test usa ids únicos.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, EventoProveedorJpaMapper.class,
		EventoProveedorRepositorioJpa.class})
class EventoProveedorRepositorioJpaTest {

	private static final Instant RECIBIDO_EN = Instant.parse("2026-07-10T10:00:00Z");

	@Autowired
	private EventoProveedorRepositorio repositorio;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void unEvento_sobreviveElViajeDominioJpaSinPerdidas() {
		EventoProveedor original = EventoProveedor.registrar(
				"simulado", idUnico(), "PAGO_RECIBIDO", "{\"x\":1}", RECIBIDO_EN);
		original.marcarParaRevision("nota de prueba");

		repositorio.guardar(original);

		assertThat(repositorio.existe("simulado", original.idExternoEvento())).isTrue();
		assertThat(jdbc.queryForObject(
				"select nota_revision from eventos_proveedor where id = ?",
				String.class, original.id().valor())).isEqualTo("nota de prueba");
	}

	@Test
	void existe_distinguePorProveedorYPorId() {
		String idExterno = idUnico();
		repositorio.guardar(EventoProveedor.registrar(
				"simulado", idExterno, "PAGO_RECIBIDO", "{}", RECIBIDO_EN));

		assertThat(repositorio.existe("simulado", idExterno)).isTrue();
		assertThat(repositorio.existe("binance", idExterno)).isFalse(); // otro proveedor
		assertThat(repositorio.existe("simulado", idUnico())).isFalse(); // otro evento
	}

	@Test
	void elMismoEventoDosVeces_loDetieneLaConstraint_yElAdaptadorLoTraduce() {
		String idExterno = idUnico();
		repositorio.guardar(EventoProveedor.registrar(
				"simulado", idExterno, "PAGO_RECIBIDO", "{}", RECIBIDO_EN));

		assertThatThrownBy(() -> repositorio.guardar(EventoProveedor.registrar(
				"simulado", idExterno, "PAGO_RECIBIDO", "{}", RECIBIDO_EN)))
				.isInstanceOf(EventoDuplicadoException.class);
	}

	@Test
	void laConstraint_muerdeANivelSqlPuro_sinPasarPorJava() {
		String idExterno = idUnico();
		String insercion = """
				insert into eventos_proveedor
				(id, proveedor, id_externo_evento, tipo, carga_cruda, firma_valida, procesado, recibido_en)
				values (?, 'simulado', ?, 'PAGO_RECIBIDO', '{}', true, false, now())
				""";
		jdbc.update(insercion, UUID.randomUUID(), idExterno);

		assertThatThrownBy(() -> jdbc.update(insercion, UUID.randomUUID(), idExterno))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void variosIntentosConFirmaInvalida_convivenSinChocar() {
		// id_externo_evento NULL: el índice único de PostgreSQL los ignora
		repositorio.guardar(EventoProveedor.registrarIntentoConFirmaInvalida(
				"simulado", "{}", RECIBIDO_EN));
		repositorio.guardar(EventoProveedor.registrarIntentoConFirmaInvalida(
				"simulado", "{}", RECIBIDO_EN));

		assertThat(jdbc.queryForObject(
				"select count(*) from eventos_proveedor where proveedor = 'simulado' and id_externo_evento is null",
				Integer.class)).isGreaterThanOrEqualTo(2);
	}

	private static String idUnico() {
		return "evt-" + UUID.randomUUID();
	}

}
