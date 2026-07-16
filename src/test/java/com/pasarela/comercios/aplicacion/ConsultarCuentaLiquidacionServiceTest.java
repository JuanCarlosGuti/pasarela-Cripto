package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioNoEncontradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import com.pasarela.compartido.dominio.puerto.ConsultorDeCuentaLiquidacion.DatosCuentaLiquidacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarCuentaLiquidacionServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-15T15:00:00Z");

	@Mock
	private ComercioRepositorio repositorio;

	private ConsultarCuentaLiquidacionService servicio;
	private Comercio comercio;

	@BeforeEach
	void configurar() {
		servicio = new ConsultarCuentaLiquidacionService(repositorio);
		comercio = Comercio.registrar(
				"Tienda Doña Rosa", Nit.de("899999068-1"),
				new CuentaLiquidacion(TipoCuenta.NEQUI, "3001234567", "Doña Rosa"),
				AHORA.minusSeconds(3600));
	}

	@Test
	void devuelveLaCuentaDelComercio() {
		when(repositorio.buscarPorId(comercio.id())).thenReturn(Optional.of(comercio));

		DatosCuentaLiquidacion datos = servicio.obtener(comercio.id());

		assertThat(datos.tipoCuenta()).isEqualTo("NEQUI");
		assertThat(datos.numero()).isEqualTo("3001234567");
		assertThat(datos.titular()).isEqualTo("Doña Rosa");
	}

	@Test
	void unComercioInexistente_lanza404DeDominio() {
		when(repositorio.buscarPorId(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.obtener(comercio.id()))
				.isInstanceOf(ComercioNoEncontradoException.class);
	}

}
