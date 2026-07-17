package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.EstadoVerificacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.ConsultarComerciosUseCase.PaginaDeComercios;
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
	void sinEstado_devuelveLaPaginaPedida_conElTotal() {
		Comercio comercio = comercio("899999068-1");
		when(repositorio.listar(0, 20)).thenReturn(
				new ComercioRepositorio.PaginaDeComercios(List.of(comercio), 41));

		PaginaDeComercios pagina = servicio.listar(null, 0, 20);

		assertThat(pagina.comercios()).containsExactly(comercio);
		assertThat(pagina.totalElementos()).isEqualTo(41);
		assertThat(pagina.pagina()).isZero();
		assertThat(pagina.tamano()).isEqualTo(20);
	}

	@Test
	void conEstado_filtraPorEseEstado() {
		Comercio pendiente = comercio("899999068-1");
		when(repositorio.listarPorEstado(EstadoVerificacion.PENDIENTE, 1, 10)).thenReturn(
				new ComercioRepositorio.PaginaDeComercios(List.of(pendiente), 11));

		PaginaDeComercios pagina = servicio.listar("PENDIENTE", 1, 10);

		assertThat(pagina.comercios()).containsExactly(pendiente);
		verify(repositorio).listarPorEstado(EstadoVerificacion.PENDIENTE, 1, 10);
	}

	@Test
	void unaPaginaNegativaOUnTamanoAbsurdo_seNormalizan() {
		when(repositorio.listar(0, 100)).thenReturn(
				new ComercioRepositorio.PaginaDeComercios(List.of(), 0));

		PaginaDeComercios pagina = servicio.listar("", -3, 5000);

		assertThat(pagina.pagina()).isZero();
		assertThat(pagina.tamano()).isEqualTo(100);
		verify(repositorio).listar(0, 100);
	}

	@Test
	void unEstadoQueNoExiste_lanza400DeDominio_sinTocarElRepositorio() {
		assertThatThrownBy(() -> servicio.listar("INVENTADO", 0, 20))
				.isInstanceOf(ComercioInvalidoException.class)
				.hasMessageContaining("INVENTADO");
		verifyNoInteractions(repositorio);
	}

	private static Comercio comercio(String nit) {
		return Comercio.registrar("Tienda", Nit.de(nit),
				new CuentaLiquidacion("Nequi", TipoCuenta.AHORROS, "3001234567", "Tienda"),
				AHORA.minusSeconds(3600));
	}

}
