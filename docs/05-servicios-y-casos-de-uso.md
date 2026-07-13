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
| `RegistrarComercioUseCase` | Alta de un comercio (queda `PENDIENTE`); crea también la cuenta de acceso del dueño vía el puerto compartido `CuentasDeAccesoPort` | Comercio |
| `DecidirVerificacionUseCase` | Aprobar/rechazar/suspender/reactivar un comercio | Admin |
| `ConsultarComercioUseCase` | Ver datos y estado del comercio | Comercio/Admin |
| `ActualizarLimitesUseCase` | Ajustar topes de operación | Admin |

### Contexto Seguridad

| Caso de uso | Descripción | Actor |
|-------------|-------------|-------|
| `AutenticarUsuarioUseCase` | Login: valida credenciales y emite JWT (401 idéntico si el usuario no existe) | Comercio/Admin |

### Contexto Pagos

| Caso de uso | Descripción | Actor |
|-------------|-------------|-------|
| `CrearOrdenUseCase` | Generar un cobro en COP + QR (autoriza contra `comercios` vía `AutorizadorDeCobros`) | Comercio |
| `ConsultarOrdenUseCase` | Ver estado de una orden | Comercio/Pagador |
| `ConsultarPagoPublicoUseCase` | Consulta pública por referencia (solo estado y monto) | Pagador |
| `ProcesarWebhookUseCase` | Procesar confirmación del proveedor (idempotente) | Sistema (webhook) |
| `ExpirarOrdenesVencidasUseCase` | Expirar órdenes vencidas sin pago | Sistema (job) |
| `ReconciliarOrdenesUseCase` | Revisar órdenes pendientes por si se perdió un webhook; confirma por la MISMA ruta que el webhook | Sistema (job) |
| `ConsultarVentasUseCase` | Dashboard: ventas del día/mes y listado paginado | Comercio |
| `ExportarVentasUseCase` | Exportar historial a CSV (HU-019) | Comercio |

### Contexto Liquidaciones

| Caso de uso | Descripción | Actor |
|-------------|-------------|-------|
| `RegistrarLiquidacionUseCase` | Registrar liquidación reportada por el proveedor | Sistema |
| `ConciliarLiquidacionUseCase` | Conciliar registrado vs. reportado; discrepancia con detalle + alerta | Sistema/Admin |

---

## Puertos de salida (lo que el dominio necesita del exterior)

Definidos en `dominio/puerto/salida`, implementados en `infraestructura`.

### Repositorios

```java
public interface OrdenDePagoRepositorio {
    OrdenDePago guardar(OrdenDePago orden);
    Optional<OrdenDePago> buscarPorId(IdOrden id);
    Optional<OrdenDePago> buscarPorReferencia(ReferenciaPago referencia);
    List<OrdenDePago> buscarPendientesExpiradas(Instant ahora, int limite);
    List<OrdenDePago> buscarPendientesCreadasAntesDe(Instant limite, int maximo);
    Dinero acumuladoDelMes(IdComercio comercioId, Instant desde, Instant hasta);
    VentasTotalizadas totalizarVentas(IdComercio comercioId, Instant desde, Instant hasta, Set<EstadoOrden> estados);
    PaginaDeOrdenes listarDelComercio(IdComercio comercioId, Instant desde, Instant hasta, int pagina, int tamano);
}

public interface ComercioRepositorio {
    Comercio guardar(Comercio comercio);
    Optional<Comercio> buscarPorId(IdComercio id);
    Optional<Comercio> buscarPorNit(Nit nit);
}

public interface EventoProveedorRepositorio {
    boolean existe(String proveedor, String idExternoEvento); // idempotencia
    EventoProveedor guardar(EventoProveedor evento); // transacción PROPIA (REQUIRES_NEW)
}
```

> Firmas simplificadas para ilustrar el contrato; ver las clases reales en
> `dominio/puerto/salida` de cada contexto para la lista completa y actualizada.

### Puerto del proveedor de pago (clave de la arquitectura)

Este puerto **abstrae cualquier riel de pago**. En el MVP lo implementa
`BinancePayAdapter`; mañana, un adaptador de rampa on-chain — sin tocar el dominio.

```java
public interface ProveedorDePagoPort {

    // Crea el cobro en el proveedor y devuelve los datos para mostrar el QR/checkout.
    CobroCreado crearCobro(SolicitudDeCobro solicitud);

    // Valida la firma de un webhook entrante (HMAC, comparación en tiempo constante).
    boolean firmaValida(String cargaCruda, String firma);

    // Traduce el payload crudo del proveedor al lenguaje del dominio.
    WebhookDelProveedor interpretarWebhook(String cargaCruda);

    // Consulta activa del estado de un cobro (HU-015, reconciliación): devuelve
    // el pago en el MISMO formato del webhook (carga + firma), para confirmarlo
    // por la ruta idempotente sin duplicar lógica.
    Optional<CobroConsultado> consultarCobro(ReferenciaPago referencia, Dinero monto);
}
```

> El adaptador simulado (`ProveedorDePagoSimulado`) implementa el puerto completo;
> el adaptador real de Binance Pay (Sprint 7, HU-021) será el segundo (ADR-003).

### Otros puertos de salida

```java
public interface NotificadorDeComercios {
    void pagoDetectado(OrdenDePago orden); // best-effort: su fallo no revierte nada
}
```

> **Notificación real (ADR-005):** no es push en tiempo real. El adaptador de hoy
> (`NotificadorPorLog`) solo deja traza en el log; el frontend se entera por
> **polling corto (2-3 s)** sobre `GET /api/ordenes/{id}` (caja del comercio) y
> `GET /api/pagos/{referencia}` (página del pagador), ambos con
> `Cache-Control: no-store`. Ver ADR-005 para el porqué (cero infraestructura
> nueva; el puerto queda como punto de extensión si el piloto exige SSE/push).

---

## Flujo detallado: procesar webhook de pago (el más crítico)

`ProcesarWebhookUseCase` — debe ser **idempotente** y robusto:

```
1. Recibe el webhook crudo (cargaCruda + firma).

2. Valida la firma con el proveedor (ProveedorDePagoPort.firmaValida).
   └─ Si es inválida → registrar el intento (firmaValida=false) y responder 401.

3. Interpreta el webhook → WebhookDelProveedor (idExternoEvento, referencia, monto...).

4. Idempotencia: ¿ya existe (proveedor, idExternoEvento)?
   └─ Sí → no hacer nada, responder 200 OK con resultado DUPLICADO.
   └─ No → continuar.

5. Guarda el EventoProveedor CRUDO (transacción PROPIA, REQUIRES_NEW: sobrevive
   aunque el resto del procesamiento falle o se rechace).
   └─ Si la constraint única lo detiene (carrera con otro hilo) → DUPLICADO.

6. Busca la OrdenDePago por su referencia.
   └─ No existe → evento a revisión + alerta en bitácora, responde 200 (PARA_REVISION).

7. Camino triste: monto distinto al esperado → orden a EN_REVISION (vía FALLIDA
   con motivo), evento a revisión + alerta, responde 200 (PARA_REVISION).

8. Aplica la regla de dominio: orden.confirmarPago(evento).
   └─ Si la transición no es válida (pago tardío tras expiración) → evento a
      revisión con todos los datos para el reembolso, responde 200 (PARA_REVISION).
   └─ Si pierde la carrera optimista contra el job de expiración (HU-014) →
      recarga la orden y reintenta UNA vez por el mismo camino.

9. Persiste la orden y marca el evento como procesado.

10. Notifica al comercio (NotificadorDeComercios, best-effort — ver ADR-005).

11. Responde 200 OK al proveedor con resultado CONFIRMADO.
```

> Nunca se responde 5xx salvo firma inválida (401): un error de servidor
> provocaría reintentos infinitos del proveedor (docs/07).

> **Regla de oro del webhook:** procesar el mismo evento N veces produce **exactamente
> el mismo resultado** que procesarlo una vez. Nunca doble cobro, nunca doble
> liquidación.

---

## Jobs programados (respaldo)

- **`ExpirarOrdenesVencidasUseCase`** — corre periódicamente (job por lotes, cada
  orden en su propia transacción); expira órdenes `PENDIENTE_PAGO` vencidas. Usa
  bloqueo optimista (`@Version`) para resolver la carrera contra un pago simultáneo:
  si pierde, cede — el pago gana.
- **`ReconciliarOrdenesUseCase`** — corre periódicamente; para órdenes `PENDIENTE_PAGO`
  atascadas, consulta al proveedor (`ProveedorDePagoPort.consultarCobro`) y, si
  reporta el pago, lo confirma metiéndolo por `ProcesarWebhookUseCase` — la MISMA
  ruta idempotente del webhook, sin duplicar lógica (cierra ADR-004).

Estos jobs son la red de seguridad ante fallos de red o webhooks perdidos: el sistema
**converge al estado correcto** aunque un webhook nunca llegue.

---

## Manejo de errores por capa

- **Dominio:** lanza excepciones de negocio con significado
  (`OrdenNoPuedeConfirmarseException`).
- **Aplicación:** traduce/propaga; decide compensaciones si aplica.
- **Infraestructura (REST):** un `@ControllerAdvice` traduce excepciones a respuestas
  HTTP apropiadas, **sin filtrar detalles internos**.
