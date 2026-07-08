package com.pasarela.comercios.infraestructura.salida.persistencia;

import com.pasarela.TestcontainersConfiguration;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ComercioJpaMapper.class, ComercioRepositorioJpa.class})
class ComercioRepositorioJpaTest {

	private static final Instant AHORA = Instant.parse("2026-07-08T10:00:00Z");

	@Autowired
	private ComercioRepositorio repositorio;

	@Test
	void unComercio_sobreviveElViajeDominioJpaSinPerdidas() {
		Comercio original = comercio("899999068-1");

		repositorio.guardar(original);
		Comercio recuperado = repositorio.buscarPorId(original.id()).orElseThrow();

		assertThat(recuperado.id()).isEqualTo(original.id());
		assertThat(recuperado.razonSocial()).isEqualTo(original.razonSocial());
		assertThat(recuperado.nit()).isEqualTo(original.nit());
		assertThat(recuperado.cuentaLiquidacion()).isEqualTo(original.cuentaLiquidacion());
		assertThat(recuperado.estadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
		assertThat(recuperado.registradoEn()).isEqualTo(AHORA);
	}

	@Test
	void buscarPorNit_devuelveElComercioCorrecto_noOtro() {
		Comercio buscado = comercio("899999068-1");
		Comercio otro = comercio("890903938-8"); // falso positivo potencial
		repositorio.guardar(buscado);
		repositorio.guardar(otro);

		assertThat(repositorio.buscarPorNit(Nit.de("899999068-1")).orElseThrow().id())
				.isEqualTo(buscado.id());
		assertThat(repositorio.buscarPorNit(Nit.de("800197268-4"))).isEmpty();
	}

	@Test
	void dosComerciosConElMismoNit_fallanPorConstraintDeBaseDeDatos() {
		repositorio.guardar(comercio("899999068-1"));

		assertThatThrownBy(() -> repositorio.guardar(comercio("899999068-1")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static Comercio comercio(String nit) {
		return Comercio.registrar(
				"Tienda " + nit,
				Nit.de(nit),
				new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", "Tienda " + nit),
				AHORA);
	}

}