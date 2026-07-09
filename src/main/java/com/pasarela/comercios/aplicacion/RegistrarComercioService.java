package com.pasarela.comercios.aplicacion;

import com.pasarela.comercios.dominio.excepcion.ComercioInvalidoException;
import com.pasarela.comercios.dominio.excepcion.ComercioYaRegistradoException;
import com.pasarela.comercios.dominio.modelo.Comercio;
import com.pasarela.comercios.dominio.modelo.CuentaLiquidacion;
import com.pasarela.comercios.dominio.modelo.Nit;
import com.pasarela.comercios.dominio.modelo.TipoCuenta;
import com.pasarela.comercios.dominio.puerto.entrada.RegistrarComercioUseCase;
import com.pasarela.comercios.dominio.puerto.salida.ComercioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Implementa el registro del comercio (HU-004): construye los VOs (que se
 * validan solos), rechaza NITs duplicados y persiste el comercio PENDIENTE.
 * El servicio coordina; las reglas viven en el dominio.
 */
@Service
public class RegistrarComercioService implements RegistrarComercioUseCase {

	private final ComercioRepositorio repositorio;
	private final Clock reloj;

	public RegistrarComercioService(ComercioRepositorio repositorio, Clock reloj) {
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public Comercio registrar(ComandoRegistrarComercio comando) {
		Nit nit = Nit.de(comando.nit());
		CuentaLiquidacion cuenta = new CuentaLiquidacion(
				tipoDeCuenta(comando.tipoCuenta()),
				comando.numeroCuenta(),
				comando.titularCuenta());
		if (repositorio.buscarPorNit(nit).isPresent()) {
			throw new ComercioYaRegistradoException(
					"Ya existe un comercio registrado con el NIT " + nit.completo());
		}
		Comercio comercio = Comercio.registrar(
				comando.razonSocial(), nit, cuenta, reloj.instant());
		return repositorio.guardar(comercio);
	}

	private static TipoCuenta tipoDeCuenta(String texto) {
		try {
			return TipoCuenta.valueOf(texto == null ? "" : texto.trim().toUpperCase());
		} catch (IllegalArgumentException excepcion) {
			throw new ComercioInvalidoException(
					"El tipo de cuenta no es válido; use NEQUI, AHORROS o CORRIENTE");
		}
	}

}