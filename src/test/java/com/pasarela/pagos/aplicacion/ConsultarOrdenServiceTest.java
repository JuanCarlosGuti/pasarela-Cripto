package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.pagos.dominio.excepcion.OrdenNoEncontradaException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.ConsultarOrdenUseCase.ComandoConsultarOrden;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarOrdenServiceTest {

	private static final Instant AHORA = Instant.parse("2026-07-09T15:00:00Z");

	@Mock
	private OrdenDePagoRepositorio repositorio;

	private ConsultarOrdenService servicio;
	private OrdenDePago orden;

	@BeforeEach
	void configurar() {
		servicio = new ConsultarOrdenService(repositorio);
		orden = OrdenDePago.crear(
				IdComercio.generar(), Dinero.cop(40000), ReferenciaPago.generar(),
				AHORA, AHORA.plus(Duration.ofMinutes(15)));
	}

	@Test
	void elDueno_puedeConsultarSuOrden() {
		when(repositorio.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

		OrdenDePago consultada = servicio.consultar(new ComandoConsultarOrden(
				orden.id().valor(), orden.comercioId().valor()));

		assertThat(consultada).isEqualTo(orden);
	}

	@Test
	void unAdmin_puedeConsultarCualquierOrden() {
		when(repositorio.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

		assertThat(servicio.consultar(new ComandoConsultarOrden(orden.id().valor(), null)))
				.isEqualTo(orden);
	}

	@Test
	void otroComercio_recibeNoEncontrada_igualQueUnaInexistente() {
		when(repositorio.buscarPorId(orden.id())).thenReturn(Optional.of(orden));
		String mensajeAjena = mensajeDe(() -> servicio.consultar(
				new ComandoConsultarOrden(orden.id().valor(), UUID.randomUUID())));

		when(repositorio.buscarPorId(any())).thenReturn(Optional.empty());
		String mensajeInexistente = mensajeDe(() -> servicio.consultar(
				new ComandoConsultarOrden(UUID.randomUUID(), UUID.randomUUID())));

		// mismo mensaje exacto: no se filtra existencia
		assertThat(mensajeAjena).isEqualTo(mensajeInexistente);
	}

	private String mensajeDe(Runnable accion) {
		try {
			accion.run();
			throw new AssertionError("Se esperaba OrdenNoEncontradaException");
		} catch (OrdenNoEncontradaException excepcion) {
			return excepcion.getMessage();
		}
	}

}
