package com.pasarela.pagos.dominio.puerto.entrada;

import com.pasarela.pagos.dominio.modelo.OrdenDePago;
import com.pasarela.pagos.dominio.puerto.salida.ProveedorDePagoPort.CobroCreado;

import java.util.UUID;

/**
 * Caso de uso: un comercio verificado crea un cobro y obtiene el QR
 * (HU-008). Montos en pesos (COP).
 */
public interface CrearOrdenUseCase {

	OrdenCreada crear(ComandoCrearOrden comando);

	record ComandoCrearOrden(UUID comercioId, long montoPesos) {
	}

	/** La orden persistida (PENDIENTE_PAGO) y los datos de pago del proveedor. */
	record OrdenCreada(OrdenDePago orden, CobroCreado cobro) {
	}

}
