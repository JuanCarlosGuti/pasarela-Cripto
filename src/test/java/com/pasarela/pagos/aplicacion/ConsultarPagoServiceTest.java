package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenNoEncontradaException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarPagoPublicoUseCase.EstadoDePago;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarPagoServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T15:00:00Z");

	@Mock
	private OrdenDePagoRepositorio repositorio;

	private ConsultarPagoService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ConsultarPagoService(repositorio);
	}

	@Test
	void devuelveSoloEstadoYMonto_nadaDelComercio() {
		OrdenDePago orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), new ReferenciaPago("ref-publica"),
				AHORA, AHORA.plus(Duration.ofMinutes(15)));
		orden.registrarCobroEnProveedor(AHORA);
		when(repositorio.buscarPorReferencia(new ReferenciaPago("ref-publica")))
				.thenReturn(Optional.of(orden));

		EstadoDePago estado = servicio.consultar("ref-publica");

		assertThat(estado.estado()).isEqualTo("PENDIENTE_PAGO");
		assertThat(estado.monto()).isEqualTo(Dinero.cop(40000));
	}

	@Test
	void referenciaInexistente_lanzaNoEncontrada() {
		when(repositorio.buscarPorReferencia(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.consultar("no-existe"))
				.isInstanceOf(OrdenNoEncontradaException.class);
	}

}
