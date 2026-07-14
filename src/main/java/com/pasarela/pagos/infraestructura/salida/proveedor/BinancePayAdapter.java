package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Adaptador real de Binance Pay (HU-021), segundo adaptador de
 * {@link ProveedorDePagoPort} — valida ADR-003: cambiar simulador↔Binance es
 * SOLO configuración, cero cambios en dominio y aplicación.
 *
 * <p><b>Contratos implementados según la especificación pública</b> (pendiente
 * de re-validar contra el sandbox merchant cuando llegue el acceso):</p>
 * <ul>
 *   <li><b>Peticiones salientes</b> (crear orden, consultar orden): headers
 *       {@code BinancePay-Timestamp/Nonce/Certificate-SN/Signature} con
 *       HMAC-SHA512 de {@code timestamp\nnonce\ncuerpo\n} en hex mayúsculas.</li>
 *   <li><b>Webhook entrante:</b> Binance firma con RSA (SHA256withRSA) sobre
 *       {@code timestamp\nnonce\ncuerpo\n}; la firma llega compuesta desde el
 *       controller como {@code timestamp|nonce|firmaBase64} y se verifica con
 *       la clave pública configurada.</li>
 *   <li><b>Reconciliación (HU-015):</b> el evento fabricado por consulta activa
 *       no puede llevar la firma RSA de Binance (no tenemos su clave privada);
 *       se firma internamente con nuestro secreto y el prefijo {@code recon:}.
 *       El origen que la firma certifica es nuestra propia consulta
 *       autenticada al proveedor — misma garantía, misma ruta idempotente.</li>
 * </ul>
 *
 * <p>Decisión abierta (bloqueada por sandbox): denominación del cobro — si el
 * sandbox no acepta {@code COP} directo, la cotización COP→cripto debe
 * resolverla el proveedor, nunca esta plataforma (REGLA DE ORO).</p>
 */
@Component
@ConditionalOnProperty(name = "pasarela.proveedores.binance.habilitado", havingValue = "true")
public class BinancePayAdapter implements ProveedorDePagoPort {

	static final String RUTA_CREAR_ORDEN = "/binancepay/openapi/v3/order";
	static final String RUTA_CONSULTAR_ORDEN = "/binancepay/openapi/v2/order/query";
	static final String PREFIJO_FIRMA_RECONCILIACION = "recon:";

	private final RestClient http;
	private final String apiKey;
	private final String secreto;
	private final PublicKey clavePublicaWebhook;
	private final String moneda;
	private final Clock reloj;
	private final ObjectMapper json = new ObjectMapper();

	public BinancePayAdapter(
			@Value("${pasarela.proveedores.binance.base-url}") String baseUrl,
			@Value("${pasarela.proveedores.binance.api-key}") String apiKey,
			@Value("${pasarela.proveedores.binance.secreto}") String secreto,
			@Value("${pasarela.proveedores.binance.clave-publica-webhook}") String clavePublicaWebhook,
			@Value("${pasarela.proveedores.binance.moneda:COP}") String moneda,
			@Value("${pasarela.proveedores.binance.milisegundos-timeout:5000}") long milisegundosTimeout,
			Clock reloj) {
		this.apiKey = apiKey;
		this.secreto = secreto;
		this.clavePublicaWebhook = parsearClavePublica(clavePublicaWebhook);
		this.moneda = moneda;
		this.reloj = reloj;
		SimpleClientHttpRequestFactory conexiones = new SimpleClientHttpRequestFactory();
		conexiones.setConnectTimeout(Duration.ofMillis(milisegundosTimeout));
		conexiones.setReadTimeout(Duration.ofMillis(milisegundosTimeout));
		this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(conexiones).build();
	}

	@Override
	public CobroCreado crearCobro(SolicitudDeCobro solicitud) {
		JsonNode datos = llamar(RUTA_CREAR_ORDEN, cuerpoDeCrearOrden(solicitud));
		String contenidoQr = datos.path("qrContent").asText();
		String deeplink = datos.path("deeplink").asText();
		if (contenidoQr.isBlank() || deeplink.isBlank()) {
			throw new ProveedorDePagoNoDisponibleException(
					"Binance Pay respondió SUCCESS pero sin qrContent/deeplink");
		}
		return new CobroCreado(contenidoQr, deeplink);
	}

	/**
	 * Estructura de la orden según la especificación pública v3 (terminal WEB,
	 * bien genérico). Los campos exactos exigidos se re-validan contra el
	 * sandbox; la descripción no lleva datos del comercio ni del pagador.
	 */
	private String cuerpoDeCrearOrden(SolicitudDeCobro solicitud) {
		ObjectNode orden = json.createObjectNode();
		orden.putObject("env").put("terminalType", "WEB");
		orden.put("merchantTradeNo", solicitud.referencia().valor());
		orden.put("orderAmount", solicitud.monto().monto().toPlainString());
		orden.put("currency", moneda);
		orden.put("description", "Cobro " + solicitud.referencia().valor());
		orden.putArray("goodsDetails").addObject()
				.put("goodsType", "02")
				.put("goodsCategory", "Z000")
				.put("referenceGoodsId", solicitud.referencia().valor())
				.put("goodsName", "Cobro en comercio");
		return orden.toString();
	}

	@Override
	public boolean firmaValida(String cargaCruda, String firma) {
		if (firma == null || firma.isBlank()) {
			return false;
		}
		if (firma.startsWith(PREFIJO_FIRMA_RECONCILIACION)) {
			return firmaDeReconciliacionValida(cargaCruda,
					firma.substring(PREFIJO_FIRMA_RECONCILIACION.length()));
		}
		return firmaRsaDeBinanceValida(cargaCruda, firma);
	}

	/** Firma compuesta del webhook real: {@code timestamp|nonce|firmaBase64}. */
	private boolean firmaRsaDeBinanceValida(String cargaCruda, String firmaCompuesta) {
		String[] partes = firmaCompuesta.split("\\|", 3);
		if (partes.length != 3) {
			return false;
		}
		try {
			byte[] firmaRsa = Base64.getDecoder().decode(partes[2]);
			String carga = partes[0] + "\n" + partes[1] + "\n" + cargaCruda + "\n";
			Signature verificador = Signature.getInstance("SHA256withRSA");
			verificador.initVerify(clavePublicaWebhook);
			verificador.update(carga.getBytes(StandardCharsets.UTF_8));
			return verificador.verify(firmaRsa);
		} catch (Exception firmaIlegible) {
			// base64 corrupto, firma truncada...: inválida, jamás excepción
			return false;
		}
	}

	private boolean firmaDeReconciliacionValida(String cargaCruda, String firmaHmac) {
		byte[] esperada = hmacSha512(cargaCruda).getBytes(StandardCharsets.UTF_8);
		byte[] recibida = firmaHmac.getBytes(StandardCharsets.UTF_8);
		// comparación en tiempo constante, como en el resto de adaptadores
		return MessageDigest.isEqual(esperada, recibida);
	}

	@Override
	public WebhookDelProveedor interpretarWebhook(String cargaCruda) {
		try {
			JsonNode sobre = json.readTree(cargaCruda);
			JsonNode datos = json.readTree(textoObligatorio(sobre, "data"));
			return new WebhookDelProveedor(
					textoObligatorio(sobre, "bizIdStr"),
					textoObligatorio(sobre, "bizStatus"),
					new ReferenciaPago(textoObligatorio(datos, "merchantTradeNo")),
					Dinero.cop(numeroObligatorio(datos, "totalFee")),
					Instant.ofEpochMilli(numeroObligatorio(datos, "transactTime")));
		} catch (WebhookInvalidoException excepcion) {
			throw excepcion;
		} catch (Exception excepcion) {
			throw new WebhookInvalidoException(
					"El payload del webhook de Binance Pay está malformado", excepcion);
		}
	}

	/**
	 * Consulta activa (HU-015): si el proveedor reporta la orden pagada,
	 * fabrica el evento EN EL FORMATO DEL WEBHOOK con id determinista
	 * ({@code recon-<referencia>}) y firma interna — la reconciliación
	 * confirma por la misma ruta idempotente que un webhook real.
	 */
	@Override
	public Optional<CobroConsultado> consultarCobro(ReferenciaPago referencia, Dinero monto) {
		ObjectNode consulta = json.createObjectNode();
		consulta.put("merchantTradeNo", referencia.valor());
		JsonNode datos = llamar(RUTA_CONSULTAR_ORDEN, consulta.toString());
		if (!"PAID".equals(datos.path("status").asText())) {
			return Optional.empty();
		}
		ObjectNode interno = json.createObjectNode();
		interno.put("merchantTradeNo", referencia.valor());
		interno.put("totalFee", datos.path("orderAmount").asLong());
		interno.put("currency", datos.path("currency").asText(moneda));
		interno.put("transactTime", datos.path("transactTime")
				.asLong(reloj.instant().toEpochMilli()));
		ObjectNode sobre = json.createObjectNode();
		sobre.put("bizType", "PAY");
		sobre.put("bizIdStr", "recon-" + referencia.valor());
		sobre.put("bizStatus", "PAY_SUCCESS");
		sobre.put("data", interno.toString());
		String carga = sobre.toString();
		return Optional.of(new CobroConsultado(
				carga, PREFIJO_FIRMA_RECONCILIACION + hmacSha512(carga)));
	}

	/** POST firmado; devuelve {@code data} si el proveedor respondió SUCCESS. */
	private JsonNode llamar(String ruta, String cuerpo) {
		String respuesta;
		try {
			respuesta = http.post()
					.uri(ruta)
					.contentType(MediaType.APPLICATION_JSON)
					.headers(headers -> {
						String timestamp = String.valueOf(reloj.millis());
						String nonce = FirmadorBinancePay.nonce();
						headers.set("BinancePay-Timestamp", timestamp);
						headers.set("BinancePay-Nonce", nonce);
						headers.set("BinancePay-Certificate-SN", apiKey);
						headers.set("BinancePay-Signature",
								FirmadorBinancePay.firmar(timestamp, nonce, cuerpo, secreto));
					})
					.body(cuerpo)
					.retrieve()
					.body(String.class);
		} catch (RestClientException fallo) {
			// timeout, conexión rechazada, 4xx/5xx: al llamador solo le importa
			// que el proveedor no está disponible (el caso de uso decide)
			throw new ProveedorDePagoNoDisponibleException(
					"Binance Pay no respondió correctamente en " + ruta, fallo);
		}
		try {
			JsonNode sobre = json.readTree(respuesta);
			if (!"SUCCESS".equals(sobre.path("status").asText())) {
				throw new ProveedorDePagoNoDisponibleException(
						"Binance Pay rechazó la operación en %s (código %s)"
								.formatted(ruta, sobre.path("code").asText("desconocido")));
			}
			return sobre.path("data");
		} catch (ProveedorDePagoNoDisponibleException excepcion) {
			throw excepcion;
		} catch (Exception respuestaIlegible) {
			throw new ProveedorDePagoNoDisponibleException(
					"Binance Pay devolvió una respuesta ilegible en " + ruta, respuestaIlegible);
		}
	}

	private String hmacSha512(String carga) {
		try {
			Mac mac = Mac.getInstance("HmacSHA512");
			mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
			return HexFormat.of().withUpperCase()
					.formatHex(mac.doFinal(carga.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception excepcion) {
			throw new IllegalStateException("No fue posible calcular el HMAC", excepcion);
		}
	}

	/** Acepta PEM (con encabezados) o base64 pelado de una clave pública RSA X.509. */
	private static PublicKey parsearClavePublica(String clave) {
		try {
			String base64 = clave
					.replace("-----BEGIN PUBLIC KEY-----", "")
					.replace("-----END PUBLIC KEY-----", "")
					.replaceAll("\\s", "");
			return KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
		} catch (Exception excepcion) {
			// fail-fast al arrancar: mejor no levantar que aceptar webhooks sin verificar
			throw new IllegalStateException(
					"La clave pública del webhook de Binance Pay no es válida", excepcion);
		}
	}

	private static String textoObligatorio(JsonNode nodo, String campo) {
		JsonNode valor = nodo.path(campo);
		if (valor.isMissingNode() || valor.asText().isBlank()) {
			throw new WebhookInvalidoException(
					"El webhook de Binance Pay no trae el campo obligatorio '" + campo + "'");
		}
		return valor.asText();
	}

	private static long numeroObligatorio(JsonNode nodo, String campo) {
		JsonNode valor = nodo.path(campo);
		if (!valor.isNumber()) {
			throw new WebhookInvalidoException(
					"El webhook de Binance Pay no trae el campo numérico obligatorio '"
							+ campo + "'");
		}
		return valor.asLong();
	}

}
