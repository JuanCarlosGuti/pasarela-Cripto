package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroConsultado;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroCreado;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.SolicitudDeCobro;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.WebhookDelProveedor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato del adaptador real de Binance Pay (HU-021) contra WireMock.
 *
 * <p><b>Origen de los fixtures:</b> especificación pública del proveedor.
 * Cuando llegue el acceso al sandbox merchant se re-graban con respuestas
 * reales y se ajusta lo que difiera (esa re-validación es parte del criterio
 * de aceptación de la HU; está anotada en el backlog).</p>
 */
class BinancePayAdapterContratoTest {

	private static final Instant AHORA = Instant.parse("2026-07-13T12:00:00Z");
	private static final String API_KEY = "api-key-de-prueba";
	private static final String SECRETO = "secreto-hmac-de-prueba";

	private WireMockServer binance;
	private BinancePayAdapter adaptador;

	@BeforeEach
	void levantarProveedorFalso() throws Exception {
		binance = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		binance.start();
		adaptador = adaptadorContra(binance.baseUrl(), 2000);
	}

	@AfterEach
	void apagarProveedorFalso() {
		binance.stop();
	}

	private static BinancePayAdapter adaptadorContra(String baseUrl, long timeoutMs)
			throws Exception {
		KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		KeyPair claves = generador.generateKeyPair();
		return new BinancePayAdapter(
				baseUrl, API_KEY, SECRETO,
				Base64.getEncoder().encodeToString(claves.getPublic().getEncoded()),
				"COP", timeoutMs, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	// --- crear cobro ---

	@Test
	void crearCobro_exitoso_devuelveQrYDeeplink_yLaPeticionVaFirmada() {
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CREAR_ORDEN))
				.willReturn(okJson("""
						{"status":"SUCCESS","code":"000000","data":{
						  "prepayId":"363289398107924480",
						  "qrContent":"https://qr.binance.com/pagar/363289398107924480",
						  "deeplink":"bnc://app.binance.com/payment/secpay?x=1",
						  "checkoutUrl":"https://pay.binance.com/checkout/x",
						  "expireTime":1760000000000}}
						""")));

		CobroCreado cobro = adaptador.crearCobro(new SolicitudDeCobro(
				new ReferenciaPago("ref-contrato-1"), Dinero.cop(68000),
				AHORA.plus(Duration.ofMinutes(15))));

		assertThat(cobro.contenidoQr()).isEqualTo("https://qr.binance.com/pagar/363289398107924480");
		assertThat(cobro.deeplink()).startsWith("bnc://");

		// la petición salió con el contrato de firma del proveedor
		var peticion = binance.getAllServeEvents().getFirst().getRequest();
		String timestamp = peticion.getHeader("BinancePay-Timestamp");
		String nonce = peticion.getHeader("BinancePay-Nonce");
		assertThat(timestamp).isEqualTo(String.valueOf(AHORA.toEpochMilli()));
		assertThat(nonce).hasSize(32);
		assertThat(peticion.getHeader("BinancePay-Certificate-SN")).isEqualTo(API_KEY);
		// la firma se recomputa desde lo que REALMENTE viajó: si el cuerpo o los
		// headers cambian sin refirmar, este assert muere
		assertThat(peticion.getHeader("BinancePay-Signature")).isEqualTo(
				FirmadorBinancePay.firmar(timestamp, nonce, peticion.getBodyAsString(), SECRETO));
		assertThat(peticion.getBodyAsString())
				.contains("\"merchantTradeNo\":\"ref-contrato-1\"")
				.contains("\"orderAmount\":\"68000\"")
				.contains("\"currency\":\"COP\"");
	}

	@Test
	void crearCobro_conRechazoDelProveedor_lanzaNoDisponibleConElCodigo() {
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CREAR_ORDEN))
				.willReturn(okJson(
						"{\"status\":\"FAIL\",\"code\":\"400201\",\"errorMessage\":\"merchant not found\"}")));

		assertThatThrownBy(() -> adaptador.crearCobro(solicitudCualquiera()))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class)
				.hasMessageContaining("400201");
	}

	@Test
	void crearCobro_con500DelProveedor_lanzaNoDisponible() {
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CREAR_ORDEN))
				.willReturn(serverError()));

		assertThatThrownBy(() -> adaptador.crearCobro(solicitudCualquiera()))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class);
	}

	@Test
	void crearCobro_conTimeout_lanzaNoDisponible() throws Exception {
		BinancePayAdapter impaciente = adaptadorContra(binance.baseUrl(), 150);
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CREAR_ORDEN))
				.willReturn(okJson("{\"status\":\"SUCCESS\",\"data\":{}}")
						.withFixedDelay(1500)));

		assertThatThrownBy(() -> impaciente.crearCobro(solicitudCualquiera()))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class);
	}

	@Test
	void crearCobro_conRespuestaInesperada_lanzaNoDisponible() {
		// SUCCESS pero sin los campos del QR: payload inesperado del criterio
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CREAR_ORDEN))
				.willReturn(okJson("{\"status\":\"SUCCESS\",\"code\":\"000000\",\"data\":{}}")));
		assertThatThrownBy(() -> adaptador.crearCobro(solicitudCualquiera()))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class)
				.hasMessageContaining("qrContent");

		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CREAR_ORDEN))
				.willReturn(aResponse().withStatus(200).withBody("esto no es json")));
		assertThatThrownBy(() -> adaptador.crearCobro(solicitudCualquiera()))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class);
	}

	// --- consulta activa (reconciliación HU-015) ---

	@Test
	void consultarCobro_pagado_fabricaUnEventoQueSeConfirmaPorLaMismaRutaDelWebhook() {
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CONSULTAR_ORDEN))
				.willReturn(okJson("""
						{"status":"SUCCESS","code":"000000","data":{
						  "merchantTradeNo":"ref-recon-1","status":"PAID",
						  "orderAmount":45000,"currency":"COP",
						  "transactTime":1760350000123}}
						""")));

		Optional<CobroConsultado> consultado = adaptador.consultarCobro(
				new ReferenciaPago("ref-recon-1"), Dinero.cop(45000));

		assertThat(consultado).isPresent();
		// el invariante de HU-015: lo consultado pasa por la MISMA ruta que un
		// webhook — su firma valida y su payload se interpreta sin trato especial
		assertThat(adaptador.firmaValida(consultado.get().cargaCruda(),
				consultado.get().firma())).isTrue();
		WebhookDelProveedor evento = adaptador.interpretarWebhook(consultado.get().cargaCruda());
		assertThat(evento.idExternoEvento()).isEqualTo("recon-ref-recon-1"); // determinista
		assertThat(evento.tipo()).isEqualTo("PAY_SUCCESS");
		assertThat(evento.referencia().valor()).isEqualTo("ref-recon-1");
		assertThat(evento.monto()).isEqualTo(Dinero.cop(45000));
		assertThat(evento.pagadoEn()).isEqualTo(Instant.ofEpochMilli(1760350000123L));
	}

	@Test
	void consultarCobro_aunSinPagar_esVacio() {
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CONSULTAR_ORDEN))
				.willReturn(okJson("""
						{"status":"SUCCESS","code":"000000","data":{
						  "merchantTradeNo":"ref-recon-2","status":"PENDING"}}
						""")));

		assertThat(adaptador.consultarCobro(new ReferenciaPago("ref-recon-2"), Dinero.cop(1000)))
				.isEmpty();
	}

	@Test
	void consultarCobro_conProveedorCaido_lanzaNoDisponible() {
		binance.stubFor(post(urlEqualTo(BinancePayAdapter.RUTA_CONSULTAR_ORDEN))
				.willReturn(serverError()));

		assertThatThrownBy(() -> adaptador.consultarCobro(
				new ReferenciaPago("ref-recon-3"), Dinero.cop(1000)))
				.isInstanceOf(ProveedorDePagoNoDisponibleException.class);
	}

	private static SolicitudDeCobro solicitudCualquiera() {
		return new SolicitudDeCobro(ReferenciaPago.generar(), Dinero.cop(10000),
				AHORA.plus(Duration.ofMinutes(15)));
	}

}
