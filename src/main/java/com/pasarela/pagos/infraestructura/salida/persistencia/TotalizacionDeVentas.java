package com.pasarela.pagos.infraestructura.salida.persistencia;

import java.math.BigDecimal;

/** Fila de la agregación de ventas (expresión constructora de JPQL). */
public record TotalizacionDeVentas(BigDecimal total, long cantidad) {
}
