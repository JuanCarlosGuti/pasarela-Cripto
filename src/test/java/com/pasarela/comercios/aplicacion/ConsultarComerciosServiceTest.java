package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarComerciosServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");

	@Mock
	private ComercioRepositorio repositorio;

	private ConsultarComerciosService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ConsultarComerciosService(repositorio);
	}

	@Test
	void sinEstado_devuelveTodosLosComercios() {
		Comercio comercio = comercio("899999068-1");
		when(repositorio.listar()).thenReturn(List.of(comercio));

		assertThat(servicio.listar(null)).containsExactly(comercio);
		assertThat(servicio.listar("  ")).containsExactly(comercio);
	}

	@Test
	void conEstado_filtraPorEseEstado() {
		Comercio pendiente = comercio("899999068-1");
		when(repositorio.listarPorEstado(EstadoVerificacion.PENDIENTE))
				.thenReturn(List.of(pendiente));

		assertThat(servicio.listar("PENDIENTE")).containsExactly(pendiente);
		verify(repositorio).listarPorEstado(EstadoVerificacion.PENDIENTE);
	}

	@Test
	void unEstadoQueNoExiste_lanza400DeDominio_sinTocarElRepositorio() {
		assertThatThrownBy(() -> servicio.listar("INVENTADO"))
				.isInstanceOf(ComercioInvalidoException.class)
				.hasMessageContaining("INVENTADO");
		verifyNoInteractions(repositorio);
	}

	private static Comercio comercio(String nit) {
		return Comercio.registrar("Tienda", Nit.de(nit),
				new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", "Tienda"),
				AHORA.minusSeconds(3600));
	}

}
