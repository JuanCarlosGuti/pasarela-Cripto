package com.pasarela.pagos.aplicacion;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.entrada.ExpirarOrdenesVencidasUseCase;
import com.pasarela.pagos.dominio.puerto.salida.OrdenDePagoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Job de expiración (HU-014). Deliberadamente SIN transacción de clase:
 * cada orden se guarda en su propia transacción (el adaptador usa
 * REQUIRES_NEW), así una carrera perdida no afecta a las demás del lote.
 *
 * <p>La carrera expiración-vs-pago la resuelve el bloqueo optimista: si un
 * webhook confirmó la orden entre la lectura y la escritura, el guardado
 * falla con {@link OptimisticLockingFailureException} y el job la salta —
 * el pago gana, que es lo que el negocio quiere.</p>
 */
@Service
public class ExpirarOrdenesService implements ExpirarOrdenesVencidasUseCase {

	private static final Logger log = LoggerFactory.getLogger(ExpirarOrdenesService.class);
	private static final int TAMANO_DE_LOTE = 100;

	private final OrdenDePagoRepositorio repositorio;
	private final Clock reloj;

	public ExpirarOrdenesService(OrdenDePagoRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	@Override
	public ResultadoExpiracion expirarVencidas() {
		Instant ahora = reloj.instant();
		int expiradas = 0;
		int carreras = 0;
		while (true) {
			List<OrdenDePago> lote = repositorio.buscarPendientesExpiradas(ahora, TAMANO_DE_LOTE);
			if (lote.isEmpty()) {
				break;
			}
			int expiradasEnLote = 0;
			for (OrdenDePago orden : lote) {
				try {
					orden.expirar(ahora);
					repositorio.guardar(orden);
					expiradas++;
					expiradasEnLote++;
				} catch (OptimisticLockingFailureException carrera) {
					carreras++;
					log.info("La orden {} recibió su pago mientras expiraba: el pago gana",
							orden.id().valor());
				}
			}
			// lote corto = ya no hay más; cero éxitos = solo carreras, no insistir
			if (lote.size() < TAMANO_DE_LOTE || expiradasEnLote == 0) {
				break;
			}
		}
		return new ResultadoExpiracion(expiradas, carreras);
	}

}
