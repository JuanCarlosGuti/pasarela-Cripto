# 04 — Modelo de Dominio

> Lenguaje ubicuo del negocio traducido a código. Todo aquí es **puro dominio**: sin
> Spring, sin JPA, sin HTTP.

---

## Contextos delimitados (bounded contexts)

El MVP tiene cuatro contextos de negocio:

1. **Pagos** — el corazón: órdenes de pago y su ciclo de vida.
2. **Comercios** — registro, verificación y datos del comercio.
3. **Liquidaciones** — registro de liquidaciones y conciliación.
4. **Seguridad** — cuentas de acceso (`Usuario`), autenticación y roles (`ADMIN`,
   `COMERCIO`). No tiene agregado de negocio propio más allá de `Usuario`; existe
   como contexto separado para no mezclar identidad/acceso con el dominio de comercios.

---

## Contexto: Pagos

### Entidad raíz: `OrdenDePago`

Representa un cobro que un comercio le hace a un pagador. Es el **agregado raíz** del
contexto de pagos.

**Atributos:**

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `id` | `IdOrden` (VO) | Identificador único |
| `comercioId` | `IdComercio` (VO) | A qué comercio pertenece |
| `monto` | `Dinero` (VO) | Monto a cobrar, en COP |
| `referencia` | `ReferenciaPago` (VO) | Referencia única para el proveedor |
| `estado` | `EstadoOrden` (enum) | Estado actual en la máquina de estados |
| `creadaEn` | `Instant` | Momento de creación |
| `expiraEn` | `Instant` | Momento de expiración |
| `historial` | `List<TransicionEstado>` | Auditoría de transiciones |
| `version` | `Long` | Bloqueo optimista (HU-014): marca opaca que el dominio no interpreta; resuelve la carrera expiración-vs-pago. `null` si la orden aún no viene de BD. |

**Comportamiento (métodos de dominio):**

- `registrarCobroEnProveedor(ahora)` — transición `CREADA` → `PENDIENTE_PAGO`.
- `confirmarPago(EventoPago, ahora)` — transición a `PAGO_DETECTADO`. Valida que la
  orden esté `PENDIENTE_PAGO` y no expirada.
- `marcarComoConvertida(ahora)` — transición a `CONVERTIDA`.
- `marcarComoLiquidada(ahora)` — transición a `LIQUIDADA`.
- `expirar(ahora)` — transición a `EXPIRADA` si venció sin pago.
- `marcarComoFallida(motivo, ahora)` — transición a `FALLIDA` (motivo obligatorio,
  auditado en el historial).
- `escalarARevision(ahora)` — transición `FALLIDA` → `EN_REVISION`.
- `estaExpirada(Instant ahora)` — regla: ¿venció? (el instante se inyecta siempre).
- `puedeConfirmarse()` — regla: ¿es válido confirmar en el estado actual?

> **Invariante clave:** el estado solo cambia mediante estos métodos, que validan la
> transición. Nunca hay un `setEstado()` público.

### Máquina de estados: `EstadoOrden`

```
        ┌─────────┐
        │ CREADA  │
        └────┬────┘
             │ (se genera el QR / cobro en el proveedor)
             ▼
     ┌───────────────┐   expira sin pago    ┌──────────┐
     │ PENDIENTE_PAGO│─────────────────────►│ EXPIRADA │ (terminal)
     └───────┬───────┘                      └──────────┘
             │ webhook: pago recibido
             ▼
     ┌───────────────┐   error / inválido   ┌──────────┐    revisión   ┌─────────────┐
     │ PAGO_DETECTADO│─────────────────────►│ FALLIDA  │──────────────►│ EN_REVISION │
     └───────┬───────┘                      └──────────┘               └─────────────┘
             │ el proveedor convierte a COP
             ▼
     ┌───────────────┐
     │  CONVERTIDA   │
     └───────┬───────┘
             │ COP liquidados al comercio
             ▼
     ┌───────────────┐
     │  LIQUIDADA    │ (terminal, éxito)
     └───────────────┘
```

**Transiciones válidas** (todo lo demás debe rechazarse):

| Desde | Evento | Hacia |
|-------|--------|-------|
| `CREADA` | cobro creado en proveedor | `PENDIENTE_PAGO` |
| `PENDIENTE_PAGO` | pago recibido (webhook) | `PAGO_DETECTADO` |
| `PENDIENTE_PAGO` | expiración | `EXPIRADA` |
| `PENDIENTE_PAGO` | pago inválido (p. ej. monto errado, HU-012) | `FALLIDA` |
| `PAGO_DETECTADO` | conversión confirmada | `CONVERTIDA` |
| `PAGO_DETECTADO` | error/pago inválido | `FALLIDA` |
| `CONVERTIDA` | liquidación confirmada | `LIQUIDADA` |
| `FALLIDA` | escalamiento | `EN_REVISION` |

> *Nota (HU-012):* un pago inválido puede detectarse estando la orden
> `PENDIENTE_PAGO` — el caso típico es un webhook con monto distinto al
> esperado. La orden pasa a `FALLIDA` (con motivo) y se escala de inmediato
> a `EN_REVISION` para gestión manual.

Estados **terminales:** `LIQUIDADA` (éxito), `EXPIRADA` (sin pago), `EN_REVISION`
(requiere intervención manual).

### Value Objects del contexto Pagos

- **`Dinero`** — `record Dinero(BigDecimal monto, Moneda moneda)`. Valida monto ≥ 0.
  Operaciones: `sumar`, `restar`, `porcentaje`. En el MVP, `Moneda` = COP.
- **`ReferenciaPago`** — identificador único que se envía al proveedor y regresa en el
  webhook. Permite casar el pago con la orden.
- **`IdOrden`** — envuelve el identificador (evita mezclar ids de distintas entidades).
- **`Comision`** — `record Comision(Porcentaje tasa)`. Calcula el monto de comisión
  sobre un `Dinero`.
- **`EventoPago`** — datos del pago confirmado por el proveedor (referencia, monto,
  moneda cripto, timestamp del proveedor).

### Entidad: `EventoProveedor` (registro crudo)

Guarda **cada webhook recibido tal cual llega**, antes de procesarlo. Es la red de
seguridad para idempotencia, auditoría y disputas.

| Atributo | Descripción |
|----------|-------------|
| `id` | Identificador |
| `idExternoEvento` | Id del evento según el proveedor (clave de idempotencia) |
| `tipo` | Tipo de evento (pago, reembolso, etc.) |
| `cargaCruda` | Payload original (JSON) |
| `firmaValida` | Si la firma se validó correctamente |
| `procesado` | Si ya se procesó |
| `recibidoEn` | Timestamp de recepción |

> La combinación `(proveedor, idExternoEvento)` es **única**: si llega dos veces el
> mismo evento, se detecta y no se procesa de nuevo.

---

## Contexto: Comercios

### Entidad raíz: `Comercio`

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `id` | `IdComercio` (VO) | Identificador |
| `razonSocial` | `String` | Nombre legal |
| `nit` | `Nit` (VO) | Identificación tributaria, validada |
| `estadoVerificacion` | enum | `PENDIENTE`, `VERIFICADO`, `RECHAZADO`, `SUSPENDIDO` |
| `cuentaLiquidacion` | `CuentaLiquidacion` (VO) | Dónde recibe los COP |
| `limites` | `LimitesOperacion` (VO) | Topes por transacción y por mes |

**Comportamiento:**

- `verificar()` — pasa a `VERIFICADO` (aprobación manual del admin en el MVP).
- `rechazar(motivo)` / `suspender(motivo)`.
- `puedeCobrar()` — regla: solo un comercio `VERIFICADO` y no suspendido puede crear
  órdenes.
- `validarLimite(Dinero monto)` — regla de cumplimiento: ¿el monto respeta los topes?

### Value Objects del contexto Comercios

- **`Nit`** — valida el formato y el dígito de verificación del NIT colombiano.
- **`CuentaLiquidacion`** — datos de la cuenta bancaria/Nequi del comercio (nunca de
  la plataforma).
- **`LimitesOperacion`** — tope por transacción y tope acumulado por mes.

---

## Contexto: Liquidaciones

### Entidad raíz: `Liquidacion`

Registra que el proveedor liquidó COP al comercio por una o varias órdenes. La
plataforma **no mueve** el dinero; solo lo **registra y concilia**.

| Atributo | Descripción |
|----------|-------------|
| `id` | Identificador |
| `comercioId` | Comercio liquidado |
| `ordenes` | IDs de las órdenes que agrupa (una orden no puede estar en dos liquidaciones) |
| `montoBruto` | Suma cobrada |
| `comisionPlataforma` | Comisión retenida/facturada |
| `montoNetoComercio` | Lo que recibe el comercio (por diferencia: bruto − comisión, cuadra al centavo) |
| `referenciaProveedor` | Referencia de la transferencia del proveedor |
| `estado` | `REGISTRADA`, `CONCILIADA`, `DISCREPANCIA` |
| `liquidadaEn` | Timestamp |
| `detalleDiscrepancia` | Detalle de la diferencia (monto y/o órdenes faltantes/sobrantes) cuando `estado == DISCREPANCIA`; `null` si no hay |

**Comportamiento:**

- `conciliar(ReporteDelProveedor)` — compara lo registrado contra lo reportado por el
  proveedor; pasa a `CONCILIADA` o marca `DISCREPANCIA` con el detalle completo
  (jamás se cuadra en silencio). Solo válido si el estado actual es `REGISTRADA`
  (una liquidación ya conciliada no se re-concilia — lanza `ConciliacionInvalidaException`).

---

## Kernel compartido (`compartido/dominio`)

Tipos usados por varios contextos:

- **`Dinero`**, **`Moneda`**, **`Porcentaje`** — no pertenecen a un solo contexto.
- **Tipo base para identificadores** (VOs de id).
- **Excepciones base de dominio.**

> Mantener el kernel compartido **mínimo**. Si algo pertenece claramente a un contexto,
> vive en ese contexto, no aquí.

---

## Notas de modelado

- Cada **agregado raíz** (`OrdenDePago`, `Comercio`, `Liquidacion`) es la única puerta
  de entrada a su grupo de objetos. No se modifican sus objetos internos desde fuera.
- Las **reglas de negocio viven en las entidades y VOs**, no en los servicios.
- Los servicios de aplicación **coordinan**, no deciden reglas.
- Todo cambio de estado relevante queda en el **historial/auditoría** de la entidad.
