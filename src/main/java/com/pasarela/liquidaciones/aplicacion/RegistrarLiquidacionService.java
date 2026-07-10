package com.pasarela.liquidaciones.aplicacion;

import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.IdOrden;
import com.pasarela.compartido.dominio.modelo.Porcentaje;
import com.pasarela.compartido.dominio.puerto.LiquidadorDeOrdenes;
import com.pasarela.compartido.dominio.puerto.LiquidadorDeOrdenes.OrdenLiquidable;
import com.pasarela.liquidaciones.dominio.excepcion.LiquidacionInvalidaException;
import com.pasarela.liquidaciones.dominio.excepcion.OrdenYaLiquidadaException;
import com.pasarela.liquidaciones.dominio.modelo.Liquidacion;
import com.pasarela.liquidaciones.dominio.puerto.entrada.RegistrarLiquidacionUseCase;
import com.pasarela.liquidaciones.dominio.puerto.salida.LiquidacionRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Registra la liquidación (HU-016). Orden del flujo: validar y obtener los
 * montos de las órdenes CONVERTIDA (contexto pagos, vía puerto compartido)
 * → construir la Liquidación (el dominio calcula bruto/comisión/neto) →
 * persistirla (la constraint única de orden detiene la doble liquidación)
 * → marcar las órdenes LIQUIDADA. Si el marcado fallara tras persistir, la
 * conciliación (HU-017) detecta la inconsistencia: la constraint ya impide
 * el daño real (liquidar dos veces).
 */
@Service
public class RegistrarLiquidacionService implements RegistrarLiquidacionUseCase {

	private final LiquidacionRepositorio repositorio;
	private final LiquidadorDeOrdenes liquidadorDeOrdenes;
	private final Clock reloj;
	private final Porcentaje tasaComision;

	public RegistrarLiquidacionService(LiquidacionRepositorio repositorio,
			LiquidadorDeOrdenes liquidadorDeOrdenes, Clock reloj,
			@Value("${pasarela.liquidaciones.tasa-comision:2.5}") String tasaComision) {
		this.repositorio = repositorio;
		this.liquidadorDeOrdenes = liquidadorDeOrdenes;
		this.reloj = reloj;
		this.tasaComision = Porcentaje.de(tasaComision);
	}

	@Override
	@Transactional
	public Liquidacion registrar(ComandoRegistrarLiquidacion comando) {
		if (comando.ordenes() == null || comando.ordenes().isEmpty()) {
			throw new LiquidacionInvalidaException(
					"La liquidación requiere al menos una orden");
		}
		List<IdOrden> ordenes = comando.ordenes().stream().map(IdOrden::de).toList();
		List<IdOrden> yaLiquidadas = repositorio.ordenesYaLiquidadas(ordenes);
		if (!yaLiquidadas.isEmpty()) {
			throw new OrdenYaLiquidadaException(
					"Estas órdenes ya pertenecen a otra liquidación: " + yaLiquidadas.stream()
							.map(id -> id.valor().toString()).toList());
		}
		List<OrdenLiquidable> liquidables = liquidadorDeOrdenes.obtenerConvertidas(
				IdComercio.de(comando.comercioId()), ordenes);
		Liquidacion liquidacion = Liquidacion.registrar(
				IdComercio.de(comando.comercioId()), liquidables, tasaComision,
				comando.referenciaProveedor(), reloj.instant());
		Liquidacion guardada = repositorio.guardar(liquidacion);
		liquidadorDeOrdenes.marcarComoLiquidadas(ordenes, reloj.instant());
		return guardada;
	}

}
