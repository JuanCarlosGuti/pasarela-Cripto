package com.pasarela.comercios.dominio.modelo;

import com.pasarela.comercios.dominio.excepcion.NitInvalidoException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NitTest {

	@Nested
	class NitsValidos {

		@ParameterizedTest(name = "NIT real válido: {0}")
		@ValueSource(strings = {
				"899999068-1",  // Ecopetrol
				"890903938-8",  // Bancolombia
				"900123456-8",  // dígito calculado con el algoritmo DIAN
				"800197268-4"   // DIAN
		})
		void nitsRealesConDigitoCorrecto_sonValidos(String texto) {
			Nit nit = Nit.de(texto);

			assertThat(nit.completo()).isEqualTo(texto);
		}

		@Test
		void de_ignoraEspaciosAlrededor() {
			assertThat(Nit.de("  899999068-1  ").completo()).isEqualTo("899999068-1");
		}

		@Test
		void completo_esNumeroGuionDigito() {
			Nit nit = Nit.de("890903938-8");

			assertThat(nit.numero()).isEqualTo("890903938");
			assertThat(nit.digitoVerificacion()).isEqualTo(8);
		}
	}

	@Nested
	class NitsInvalidos {

		@ParameterizedTest(name = "dígito de verificación errado: {0}")
		@ValueSource(strings = {
				"899999068-2",  // el correcto es 1
				"890903938-0",  // el correcto es 8
				"900123456-7"   // el correcto es 8
		})
		void digitoDeVerificacionErrado_lanzaExcepcionConMensajeClaro(String texto) {
			assertThatThrownBy(() -> Nit.de(texto))
					.isInstanceOf(NitInvalidoException.class)
					.hasMessageContaining("dígito de verificación");
		}

		@ParameterizedTest(name = "formato inválido: \"{0}\"")
		@ValueSource(strings = {
				"899999068",      // sin dígito de verificación
				"899999068-12",   // dígito de dos cifras
				"8999-1",         // muy corto
				"89999906A-1",    // letras
				"899999068_1"     // separador incorrecto
		})
		void formatoInvalido_lanzaExcepcion(String texto) {
			assertThatThrownBy(() -> Nit.de(texto))
					.isInstanceOf(NitInvalidoException.class);
		}

		@ParameterizedTest
		@NullAndEmptySource
		void nuloOVacio_lanzaExcepcion(String texto) {
			assertThatThrownBy(() -> Nit.de(texto))
					.isInstanceOf(NitInvalidoException.class);
		}
	}

}