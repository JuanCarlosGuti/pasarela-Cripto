package com.pasarela.pagos.infraestructura.salida.proveedor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.pagos.dominio.excepcion.ProveedorDePagoNoDisponibleException;
import com.pasarela.pagos.dominio.excepcion.WebhookInvalidoException;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Proveedor simulado (T-006): adaptador de {@link ProveedorDePagoPort} para
 * desarrollar todo el MVP sin el sandbox de Binance.
 *
 * <ul>
 *   <li><b>Activación:</b> solo existe si
 *       {@code pasarela.proveedores.simulado.habilitado=true} (perfiles
 *       local y test). Producción jamás define la propiedad: el bean no se
 *       crea y, sin adaptador del puerto, el arranque falla rápido — mejor
 *       que un simulador aceptando cobros reales.</li>
 *   <li><b>Fallos configurables</b> para tests de resiliencia
 *       ({@code pasarela.proveedores.simulado.modo-fallo}): ERROR simula un
 *       500 del proveedor; TIMEOUT espera y falla como una conexión
 *       vencida.</li>
 * </ul>
 *
 * <p>La validación de firma y la interpretación de webhooks se agregan
 * cuando el puerto crezca en el Sprint 4 (HU-010).</p>
 */
@Component
@ConditionalOnProperty(name = "pasarela.proveedores.simulado.habilitado", havingValue = "true")
public class ProveedorDePagoSimulado implements ProveedorDePagoPort {

	public enum ModoDeFallo {
		NINGUNO, ERROR, TIMEOUT
	}

	/** Qué responde la consulta activa de la reconciliación (HU-015). */
	public enum ResultadoDeConsulta {
		NO_PAGADO, PAGADO, ERROR
	}

	private final ModoDeFallo modoDeFallo;
	private final long milisegundosTimeout;
	private final String secretoWebhook;
	private final ResultadoDeConsulta resultadoDeConsulta;
	private final ObjectMapper json = new ObjectMapper();

	public ProveedorDePagoSimulado(
			@Value("${pasarela.proveedores.simulado.modo-fallo:NINGUNO}") ModoDeFallo modoDeFallo,
			@Value("${pasarela.proveedores.simulado.milisegundos-timeout:200}") long milisegundosTimeout,
			@Value("${pasarela.proveedores.simulado.secreto-webhook}") String secretoWebhook,
			@Value("${pasarela.proveedores.simulado.resultado-consulta:NO_PAGADO}") ResultadoDeConsulta resultadoDeConsulta) {
		this.modoDeFallo = modoDeFallo;
		this.milisegundosTimeout = milisegundosTimeout;
		this.secretoWebhook = secretoWebhook;
		this.resultadoDeConsulta = resultadoDeConsulta;
	}

	@Override
	public CobroCreado crearCobro(SolicitudDeCobro solicitud) {
		switch (modoDeFallo) {
			case ERROR -> throw new ProveedorDePagoNoDisponibleException(
					"El simulador está configurado en modo ERROR (500 del proveedor)");
			case TIMEOUT -> {
				esperar();
				throw new ProveedorDePagoNoDisponibleException(
						"El simulador está configurado en modo timeout ("
								+ milisegundosTimeout + " ms)");
			}
			case NINGUNO -> { }
		}
		String referencia = solicitud.referencia().valor();
		return new CobroCreado(
				"PAGOSIM|%s|%s".formatted(referencia, solicitud.monto().monto().toPlainString()),
				"pasarela-sim://pagar/" + referencia);
	}

	private void esperar() {
		try {
			Thread.sleep(milisegundosTimeout);
		} catch (InterruptedException interrupcion) {
			Thread.currentThread().interrupt();
		}
	}

	/** HMAC-SHA256 del cuerpo con el secreto compartido, como hará Binance (HU-021). */
	@Override
	public boolean firmaValida(String cargaCruda, String firma) {
		if (firma == null || firma.isBlank()) {
			return false;
		}
		byte[] esperada = hmac(cargaCruda).getBytes(StandardCharsets.UTF_8);
		byte[] recibida = firma.getBytes(StandardCharsets.UTF_8);
		// comparación en tiempo constante: la firma no se filtra por timing
		return MessageDigest.isEqual(esperada, recibida);
	}

	@Override
	public WebhookDelProveedor interpretarWebhook(String cargaCruda) {
		try {
			JsonNode nodo = json.readTree(cargaCruda);
			return new WebhookDelProveedor(
					textoObligatorio(nodo, "idEvento"),
					textoObligatorio(nodo, "tipo"),
					new ReferenciaPago(textoObligatorio(nodo, "referencia")),
					Dinero.cop(nodo.path("monto").asLong()),
					Instant.parse(textoObligatorio(nodo, "pagadoEn")));
		} catch (WebhookInvalidoException excepcion) {
			throw excepcion;
		} catch (Exception excepcion) {
			throw new WebhookInvalidoException(
					"El payload del webhook simulado está malformado", excepcion);
		}
	}

	private static String textoObligatorio(JsonNode nodo, String campo) {
		JsonNode valor = nodo.path(campo);
		if (valor.isMissingNode() || valor.asText().isBlank()) {
			throw new WebhookInvalidoException(
					"El webhook simulado no trae el campo obligatorio '" + campo + "'");
		}
		return valor.asText();
	}

	/**
	 * Consulta activa (HU-015). En modo PAGADO fabrica el evento con id
	 * DETERMINISTA por referencia ("evt-recon-<referencia>") — igual que el
	 * proveedor real devolvería siempre el mismo evento para el mismo pago:
	 * la idempotencia hace que reconciliar dos veces no duplique nada.
	 */
	@Override
	public java.util.Optional<CobroConsultado> consultarCobro(
			com.pasarela.pagos.dominio.modelo.ReferenciaPago referencia,
			com.pasarela.compartido.dominio.modelo.Dinero monto) {
		switch (resultadoDeConsulta) {
			case ERROR -> throw new ProveedorDePagoNoDisponibleException(
					"El simulador está configurado para fallar la consulta");
			case NO_PAGADO -> {
				return java.util.Optional.empty();
			}
			case PAGADO -> {
				String carga = """
						{"idEvento": "evt-recon-%s", "tipo": "PAGO_RECIBIDO", "referencia": "%s", "monto": %s, "pagadoEn": "%s"}
						""".formatted(referencia.valor(), referencia.valor(),
								monto.monto().toPlainString(), Instant.now()).trim();
				return java.util.Optional.of(new CobroConsultado(carga, hmac(carga)));
			}
		}
		return java.util.Optional.empty();
	}

	private String hmac(String carga) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(
					secretoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(carga.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception excepcion) {
			throw new IllegalStateException("No fue posible calcular el HMAC", excepcion);
		}
	}

}
