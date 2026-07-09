package com.pasarela.pagos.aplicacion;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.puerto.AutorizadorDeCobros;
import com.pasarela.pagos.dominio.excepcion.OrdenInvalidaException;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.entrada.CrearOrdenUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroCreado;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.SolicitudDeCobro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Crea un cobro (HU-008). Orden estricto del flujo:
 * <ol>
 *   <li>validar el monto (rápido, sin tocar nada),</li>
 *   <li>autorizar contra el contexto de comercios (verificado + límites,
 *       con el acumulado del mes calendario en zona de Colombia),</li>
 *   <li>registrar el cobro en el proveedor,</li>
 *   <li>persistir la orden ya PENDIENTE_PAGO.</li>
 * </ol>
 * Si el proveedor falla, NO se persiste nada: cero órdenes fantasma
 * (decisión documentada de HU-008).
 */
@Service
public class CrearOrdenService implements CrearOrdenUseCase {

	/** El cupo mensual se corta por mes calendario del negocio, no UTC. */
	private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

	private final OrdenDePagoRepositorio repositorio;
	private final ProveedorDePagoPort proveedor;
	private final AutorizadorDeCobros autorizador;
	private final Clock reloj;
	private final Duration ventanaDePago;

	public CrearOrdenService(OrdenDePagoRepositorio repositorio,
			ProveedorDePagoPort proveedor, AutorizadorDeCobros autorizador, Clock reloj,
			@Value("${pasarela.pagos.minutos-ventana-pago:15}") long minutosVentanaPago) {
		this.repositorio = repositorio;
		this.proveedor = proveedor;
		this.autorizador = autorizador;
		this.reloj = reloj;
		this.ventanaDePago = Duration.ofMinutes(minutosVentanaPago);
	}

	@Override
	@Transactional
	public OrdenCreada crear(ComandoCrearOrden comando) {
		Dinero monto = Dinero.cop(comando.montoPesos());
		if (monto.esCero()) {
			throw new OrdenInvalidaException("El monto del cobro debe ser mayor que cero");
		}
		IdComercio comercioId = IdComercio.de(comando.comercioId());
		Instant ahora = reloj.instant();

		autorizador.autorizar(comercioId, monto, acumuladoDelMes(comercioId, ahora));

		OrdenDePago orden = OrdenDePago.crear(
				comercioId, monto, ReferenciaPago.generar(), ahora, ahora.plus(ventanaDePago));
		CobroCreado cobro = proveedor.crearCobro(
				new SolicitudDeCobro(orden.referencia(), monto, orden.expiraEn()));
		orden.registrarCobroEnProveedor(ahora);
		return new OrdenCreada(repositorio.guardar(orden), cobro);
	}

	private Dinero acumuladoDelMes(IdComercio comercioId, Instant ahora) {
		YearMonth mes = YearMonth.from(ahora.atZone(ZONA_COLOMBIA));
		Instant desde = mes.atDay(1).atStartOfDay(ZONA_COLOMBIA).toInstant();
		Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(ZONA_COLOMBIA).toInstant();
		return repositorio.acumuladoDelMes(comercioId, desde, hasta);
	}

}
