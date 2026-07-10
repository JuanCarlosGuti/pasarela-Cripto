package com.pasarela.pagos.dominio.modelo;

import com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoProveedorTest {

	private static final Instant RECIBIDO_EN = Instant.parse("2026-07-09T15:05:00Z");
	private static final String CARGA = "{\"idEvento\":\"evt-1\"}";

	@Nested
	class Registro {

		@Test
		void registrar_conFirmaValida_quedaListoParaProcesar() {
			EventoProveedor evento = EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN);

			assertThat(evento.id()).isNotNull();
			assertThat(evento.proveedor()).isEqualTo("simulado");
			assertThat(evento.idExternoEvento()).isEqualTo("evt-1");
			assertThat(evento.tipo()).isEqualTo("PAGO_RECIBIDO");
			assertThat(evento.cargaCruda()).isEqualTo(CARGA);
			assertThat(evento.firmaValida()).isTrue();
			assertThat(evento.procesado()).isFalse();
			assertThat(evento.notaRevision()).isNull();
			assertThat(evento.recibidoEn()).isEqualTo(RECIBIDO_EN);
		}

		@Test
		void registrarIntentoConFirmaInvalida_guardaLaCargaSinIdNiTipo() {
			EventoProveedor intento = EventoProveedor.registrarIntentoConFirmaInvalida(
					"simulado", CARGA, RECIBIDO_EN);

			assertThat(intento.firmaValida()).isFalse();
			assertThat(intento.idExternoEvento()).isNull();
			assertThat(intento.tipo()).isNull();
			assertThat(intento.cargaCruda()).isEqualTo(CARGA);
		}

		@Test
		void registrar_conDatosVacios_lanzaExcepcion() {
			assertThatThrownBy(() -> EventoProveedor.registrar(
					" ", "evt-1", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN))
					.isInstanceOf(WebhookInvalidoException.class);
			assertThatThrownBy(() -> EventoProveedor.registrar(
					"simulado", " ", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN))
					.isInstanceOf(WebhookInvalidoException.class);
			assertThatThrownBy(() -> EventoProveedor.registrar(
					"simulado", "evt-1", null, CARGA, RECIBIDO_EN))
					.isInstanceOf(WebhookInvalidoException.class);
			assertThatThrownBy(() -> EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", " ", RECIBIDO_EN))
					.isInstanceOf(WebhookInvalidoException.class);
			assertThatThrownBy(() -> EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", CARGA, null))
					.isInstanceOf(WebhookInvalidoException.class);
		}
	}

	@Nested
	class Procesamiento {

		@Test
		void marcarProcesado_soloConFirmaValida() {
			EventoProveedor valido = EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN);
			EventoProveedor invalido = EventoProveedor.registrarIntentoConFirmaInvalida(
					"simulado", CARGA, RECIBIDO_EN);

			valido.marcarProcesado();
			assertThat(valido.procesado()).isTrue();

			assertThatThrownBy(invalido::marcarProcesado)
					.isInstanceOf(WebhookInvalidoException.class);
			assertThat(invalido.procesado()).isFalse();
		}

		@Test
		void marcarParaRevision_exigeNota_yLaGuarda() {
			EventoProveedor evento = EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN);

			assertThatThrownBy(() -> evento.marcarParaRevision(" "))
					.isInstanceOf(WebhookInvalidoException.class);

			evento.marcarParaRevision("no existe orden con esa referencia");
			assertThat(evento.notaRevision()).isEqualTo("no existe orden con esa referencia");
		}
	}

	@Nested
	class Reconstitucion {

		@Test
		void reconstituir_restauraTalCual_ySigueRespetandoLasReglas() {
			EventoProveedor original = EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN);
			original.marcarParaRevision("nota");

			EventoProveedor reconstituido = EventoProveedor.reconstituir(
					original.id(), original.proveedor(), original.idExternoEvento(),
					original.tipo(), original.cargaCruda(), original.firmaValida(),
					original.procesado(), original.notaRevision(), original.recibidoEn());

			assertThat(reconstituido.id()).isEqualTo(original.id());
			assertThat(reconstituido.notaRevision()).isEqualTo("nota");
			assertThat(reconstituido.procesado()).isFalse();
			reconstituido.marcarProcesado();
			assertThat(reconstituido.procesado()).isTrue();
		}

		@Test
		void reconstituir_sinId_lanzaExcepcion() {
			assertThatThrownBy(() -> EventoProveedor.reconstituir(
					null, "simulado", "evt-1", "PAGO_RECIBIDO", CARGA, true, false,
					null, RECIBIDO_EN))
					.isInstanceOf(WebhookInvalidoException.class);
		}
	}

	@Nested
	class Identidad {

		@Test
		void dosEventos_conElMismoId_sonElMismoEvento() {
			EventoProveedor evento = EventoProveedor.registrar(
					"simulado", "evt-1", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN);
			EventoProveedor mismoEvento = EventoProveedor.reconstituir(
					evento.id(), evento.proveedor(), evento.idExternoEvento(), evento.tipo(),
					evento.cargaCruda(), evento.firmaValida(), evento.procesado(),
					evento.notaRevision(), evento.recibidoEn());
			EventoProveedor otro = EventoProveedor.registrar(
					"simulado", "evt-2", "PAGO_RECIBIDO", CARGA, RECIBIDO_EN);

			assertThat(evento).isEqualTo(mismoEvento).hasSameHashCodeAs(mismoEvento);
			assertThat(evento).isNotEqualTo(otro);
			assertThat(evento.hashCode()).isNotEqualTo(otro.hashCode());
			assertThat(evento).isNotEqualTo("otra cosa");
		}
	}

}
