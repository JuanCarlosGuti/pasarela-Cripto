# 05 — Servicios y Casos de Uso

> Los casos de uso son el "qué hace la aplicación". Cada uno es una unidad pequeña con
> una sola responsabilidad, en la **capa de aplicación**, implementando un **puerto de
> entrada** del dominio.

---

## Convención

- Cada caso de uso = una interfaz (puerto de entrada) + una implementación (servicio).
- Nombre del puerto: `<Accion>UseCase` (ej. `CrearOrdenUseCase`).
- Nombre del servicio: `<Accion>Service` (ej. `CrearOrdenService`).
- Entrada: un *comando* (record inmutable). Salida: un *resultado* (record) o `void`.
- La transacción (`@Transactional`) se declara en el servicio de aplicación.

```java
// Puerto de entrada (en dominio/puerto/entrada)
public interface CrearOrdenUseCase {
    ResultadoOrden crear(CrearOrdenComando comando);
}

// Servicio (en aplicacion) — implementa el puerto, orquesta el dominio y los puertos de salida
@Service
class CrearOrdenService implements CrearOrdenUseCase {
    private final ComercioRepositorio comercios;      // puerto salida
    private final OrdenDePagoRepositorio ordenes;     // puerto salida
    private final ProveedorDePagoPort proveedor;      // puerto salida
    // ... constructor con inyección ...

    @Override @Transactional
    public ResultadoOrden crear(CrearOrdenComando comando) {
        // 1. cargar y validar comercio (regla de dominio: puedeCobrar, validarLimite)
        // 2. crear OrdenDePago (dominio)
        // 3. pedir al proveedor el cobro/QR (puerto salida)
        // 4. persistir la orden (puerto salida)
        // 5. devolver resultado con datos del QR
    }
}
```

---

## Casos de uso del MVP

### Contexto Comercios

| Caso de uso | Descripción | Actor |
|-------------|-------------|-------|
| `RegistrarComercioUseCase` | Alta de un comercio (queda `PENDIENTE`) | Comercio |
| `VerificarComercioUseCase` | Aprobar/rechazar un comercio | Admin |
| `ConsultarComercioUseCase` | Ver datos y estado del comercio | Comercio/Admin |
| `ActualizarLimitesUseCase` | Ajustar topes de operación | Admin |

### Contexto Pagos

| Caso de uso | Descripción | Actor |
|-------------|-------------|-------|
| `CrearOrdenUseCase` | Generar un cobro en COP + QR | Comercio |
| `ConsultarOrdenUseCase` | Ver estado de una orden | Comercio/Pagador |
| `ProcesarWebhookPagoUseCase` | Procesar confirmación del proveedor (idempotente) | Sistema (webhook) |
| `ExpirarOrdenesUseCase` | Expirar órdenes vencidas sin pago | Sistema (job) |
| `ReconciliarOrdenesUseCase` | Revisar órdenes pendientes por si se perdió un webhook | Sistema (job) |

### Contexto Liquidaciones

| Caso de uso | Descripción | Actor |
|-------------|-------------|-------|
| `RegistrarLiquidacionUseCase` | Registrar liquidación reportada por el proveedor | Sistema |
| `ConciliarLiquidacionUseCase` | Conciliar registrado vs. reportado | Sistema/Admin |
| `ConsultarVentasUseCase` | Dashboard: ventas del comercio | Comercio |
| `ExportarMovimientosUseCase` | Exportar historial a CSV/Excel | Comercio |

---

## Puertos de salida (lo que el dominio necesita del exterior)

Definidos en `dominio/puerto/salida`, implementados en `infraestructura`.

### Repositorios

```java
public interface OrdenDePagoRepositorio {
    void guardar(OrdenDePago orden);
    Optional<OrdenDePago> buscarPorId(IdOrden id);
    Optional<OrdenDePago> buscarPorReferencia(ReferenciaPago referencia);
    List<OrdenDePago> buscarPendientesExpiradas(Instant ahora);
}

public interface ComercioRepositorio {
    void guardar(Comercio comercio);
    Optional<Comercio> buscarPorId(IdComercio id);
}

public interface EventoProveedorRepositorio {
    boolean existeEvento(String proveedor, String idExternoEvento); // idempotencia
    void guardar(EventoProveedor evento);
}
```

### Puerto del proveedor de pago (clave de la arquitectura)

Este puerto **abstrae cualquier riel de pago**. En el MVP lo implementa
`BinancePayAdapter`; mañana, un adaptador de rampa on-chain — sin tocar el dominio.

```java
public interface ProveedorDePagoPort {

    // Crea el cobro en el proveedor y devuelve los datos para mostrar el QR/checkout.
    CobroCreado crearCobro(SolicitudCobro solicitud);

    // Valida la firma de un webhook entrante.
    boolean firmaEsValida(WebhookEntrante webhook);

    // Traduce el webhook del proveedor a un evento de dominio.
    EventoPago interpretar(WebhookEntrante webhook);

    // Identifica de qué proveedor es (para el registro y la idempotencia).
    String nombre();
}
```

### Otros puertos de salida

```java
public interface NotificadorPort {
    void notificarPagoConfirmado(IdComercio comercio, IdOrden orden);
}
```

---

## Flujo detallado: procesar webhook de pago (el más crítico)

`ProcesarWebhookPagoUseCase` — debe ser **idempotente** y robusto:

```
1. Recibe el webhook crudo (WebhookEntrante).

2. Valida la firma con el proveedor (ProveedorDePagoPort.firmaEsValida).
   └─ Si es inválida → registrar y rechazar (posible intento malicioso).

3. Guarda el EventoProveedor CRUDO (antes de procesar).

4. Idempotencia: ¿ya existe (proveedor, idExternoEvento)?
   └─ Sí → no hacer nada, responder 200 OK (ya procesado).
   └─ No → continuar.

5. Interpreta el webhook → EventoPago (referencia, monto...).

6. Busca la OrdenDePago por su referencia.
   └─ No existe → registrar discrepancia, EN_REVISION.

7. Aplica la regla de dominio: orden.confirmarPago(evento).
   └─ Si la transición no es válida (ya pagada, expirada) → manejar según caso.

8. Persiste la orden y marca el evento como procesado.

9. Notifica al comercio (NotificadorPort) → "PAGADO ✓".

10. Responde 200 OK al proveedor.
```

> **Regla de oro del webhook:** procesar el mismo evento N veces produce **exactamente
> el mismo resultado** que procesarlo una vez. Nunca doble cobro, nunca doble
> liquidación.

---

## Jobs programados (respaldo)

- **`ExpirarOrdenesUseCase`** — corre periódicamente; expira órdenes `PENDIENTE_PAGO`
  vencidas.
- **`ReconciliarOrdenesUseCase`** — corre periódicamente; para órdenes que llevan
  mucho en `PENDIENTE_PAGO` o `PAGO_DETECTADO`, consulta al proveedor por si se perdió
  un webhook, y reconcilia.

Estos jobs son la red de seguridad ante fallos de red o webhooks perdidos: el sistema
**converge al estado correcto** aunque un webhook nunca llegue.

---

## Manejo de errores por capa

- **Dominio:** lanza excepciones de negocio con significado
  (`OrdenNoPuedeConfirmarseException`).
- **Aplicación:** traduce/propaga; decide compensaciones si aplica.
- **Infraestructura (REST):** un `@ControllerAdvice` traduce excepciones a respuestas
  HTTP apropiadas, **sin filtrar detalles internos**.
