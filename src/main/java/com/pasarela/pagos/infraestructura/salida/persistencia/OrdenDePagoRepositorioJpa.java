package com.pasarela.pagos.infraestructura.salida.persistencia;

import com.pasarela.compartido.dominio.modelo.Dinero;
import com.pasarela.compartido.dominio.modelo.IdComercio;
import com.pasarela.compartido.dominio.modelo.Moneda;
import com.pasarela.pagos.dominio.modelo.EstadoOrden;
import com.pasarela.pagos.dominio.modelo.IdOrden;
import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.modelo.ReferenciaPago;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link OrdenDePagoRepositorio}. */
@Repository
public class OrdenDePagoRepositorioJpa implements OrdenDePagoRepositorio {

	private final OrdenJpaRepository jpa;
	private final OrdenJpaMapper mapper;

	public OrdenDePagoRepositorioJpa(OrdenJpaRepository jpa, OrdenJpaMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	/**
	 * Transacción PROPIA (REQUIRES_NEW): si la escritura pierde la carrera
	 * optimista (expiración vs. pago, HU-014), la excepción no envenena la
	 * transacción del llamador, que puede recargar la orden y decidir de
	 * nuevo. saveAndFlush: las violaciones (referencia duplicada, versión
	 * vieja) estallan aquí, no en un flush diferido lejos del culpable.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public OrdenDePago guardar(OrdenDePago orden) {
		return mapper.aDominio(jpa.saveAndFlush(mapper.aEntidad(orden)));
	}

	@Override
	public Optional<OrdenDePago> buscarPorId(IdOrden id) {
		return jpa.findById(id.valor()).map(mapper::aDominio);
	}

	@Override
	public Optional<OrdenDePago> buscarPorReferencia(ReferenciaPago referencia) {
		return jpa.findByReferencia(referencia.valor()).map(mapper::aDominio);
	}

	@Override
	public List<OrdenDePago> buscarPendientesExpiradas(Instant ahora, int limite) {
		return jpa.findByEstadoAndExpiraEnBefore(
						EstadoOrden.PENDIENTE_PAGO.name(), ahora,
						PageRequest.of(0, limite, Sort.by("expiraEn")))
				.stream()
				.map(mapper::aDominio)
				.toList();
	}

	@Override
	public List<OrdenDePago> buscarPendientesCreadasAntesDe(Instant limite, int maximo) {
		return jpa.findByEstadoAndCreadaEnBefore(
						EstadoOrden.PENDIENTE_PAGO.name(), limite,
						PageRequest.of(0, maximo, Sort.by("creadaEn")))
				.stream()
				.map(mapper::aDominio)
				.toList();
	}

	@Override
	public Dinero acumuladoDelMes(IdComercio comercioId, Instant desde, Instant hasta) {
		return new Dinero(
				jpa.sumarMontos(comercioId.valor(), desde, hasta, ESTADOS_QUE_CONSUMEN_CUPO),
				Moneda.COP);
	}

	/** Pendientes y pagadas consumen cupo; expiradas, fallidas y en revisión no. */
	private static final List<String> ESTADOS_QUE_CONSUMEN_CUPO = List.of(
			EstadoOrden.PENDIENTE_PAGO.name(),
			EstadoOrden.PAGO_DETECTADO.name(),
			EstadoOrden.CONVERTIDA.name(),
			EstadoOrden.LIQUIDADA.name());

}