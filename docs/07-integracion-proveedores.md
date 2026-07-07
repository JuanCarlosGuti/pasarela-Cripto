# 07 — Integración de Proveedores

> Cómo se conecta la plataforma con los rieles de pago externos, manteniendo el
> dominio limpio y el modelo **sin custodia**.

---

## Principio de integración

Todo proveedor de pago se integra como un **adaptador** que implementa el puerto
`ProveedorDePagoPort` (ver Servicios y Casos de Uso). El dominio y la aplicación
**no saben** qué proveedor hay detrás.

```
   Aplicación ──► ProveedorDePagoPort (interfaz, en dominio)
                        ▲
                        │ implementa
        ┌───────────────┴───────────────┐
        │                               │
  BinancePayAdapter            RampaOnChainAdapter
  (MVP)                        (post-MVP)
```

Añadir un proveedor = **crear un adaptador nuevo**, registrar el bean, y configurar sus
credenciales. Cero cambios en el dominio.

---

## Riel del MVP: Binance Pay

### Por qué

- Cubre a la mayoría de usuarios cripto en Colombia.
- Transferencia **off-chain interna de Binance**: instantánea, sin fee de red, sin
  esperar confirmaciones on-chain, sin riesgo de monto errado (Binance valida).
- Integración más simple → camino más corto al piloto.

### Flujo de creación de cobro

```
1. La app llama a BinancePayAdapter.crearCobro(solicitud).
2. El adaptador arma la petición a la API de Binance Pay (crear orden),
   FIRMADA con la API key/secret del merchant (HMAC-SHA512).
3. Binance responde con: prepayId, URL de checkout, deeplink y contenido del QR.
4. El adaptador devuelve un CobroCreado con esos datos.
5. La página de pago (frontend) muestra el QR / deeplink.
6. El cliente paga desde su app de Binance con su saldo.
```

### Confirmación por webhook

```
1. Binance envía un webhook FIRMADO a nuestro endpoint.
2. Validamos la firma con la clave pública / secreto del merchant.
3. Guardamos el evento crudo, aplicamos idempotencia, confirmamos la orden.
   (ver "Manejo de webhooks" abajo)
```

### Consideraciones

- El saldo pagado cae en la **cuenta merchant de Binance en cripto**; la conversión a
  COP y la liquidación se resuelven con el proveedor de rampa/flujo definido (detalle a
  cerrar en la fase de integración). La plataforma **no custodia** esos fondos.
- Requiere aplicar al **programa de merchants de Binance** (pedirá los datos de la SAS;
  el **sandbox** está disponible antes del KYB de producción).
- Credenciales (API key, secret) → **variables de entorno**, nunca en el repo.

---

## Riel post-MVP: on-chain vía proveedor de rampa

> No es parte del MVP. Se documenta para diseñar el puerto de forma compatible.

Para wallets que no son Binance (MetaMask, Trust, etc.), el pago es una transferencia
blockchain. El camino recomendado (sin custodia):

```
1. El adaptador pide al proveedor de rampa una dirección de depósito ÚNICA por orden.
2. El QR contiene esa dirección + monto (formato URI, ej. EIP-681, para que la
   wallet precargue todo).
3. El cliente envía USDT/USDC.
4. El proveedor detecta el depósito, convierte y LIQUIDA COP AL COMERCIO.
5. El proveedor notifica por webhook → confirmamos la orden.
```

Decisiones de diseño para cuando llegue:

- **Limitar redes**: arrancar con USDT/USDC en 1-2 redes baratas y rápidas (Tron, BSC,
  Polygon). **Ethereum mainnet no** (fees altos matan pagos pequeños).
- **Ventana de expiración más larga** que Binance y estado "confirmando…" (on-chain
  tarda de segundos a minutos).
- **Nunca custodia propia** (no generar direcciones nuestras ni monitorear la cadena
  nosotros mismos en el MVP — eso implicaría custodia y complejidad regulatoria).

Proveedores con API en Colombia a evaluar: Koywe, Mural Pay, Bitso Business, Minteo
(COPM). Comparar: comisión, tiempo de liquidación, si liquidan a Nequi/banco,
requisitos KYB, calidad de la API. **Requisito duro:** que liquiden **directo al
comercio** (nunca a la plataforma).

---

## Manejo de webhooks (crítico y transversal)

Este es el punto más delicado de todo el sistema. Reglas obligatorias para **cualquier**
proveedor:

### 1. Validar la firma SIEMPRE

Antes de procesar cualquier cosa, verificar que el webhook realmente viene del
proveedor (firma HMAC o clave pública). Un webhook sin firma válida se registra y se
rechaza.

### 2. Guardar el evento crudo ANTES de procesar

Persistir el `EventoProveedor` con el payload original. Esto da trazabilidad para
auditoría y disputas, y es la base de la idempotencia.

### 3. Idempotencia estricta

```
clave de idempotencia = (nombreProveedor, idExternoEvento)

Si ya existe esa clave procesada:
    → no hacer nada, responder 200 OK.
Si no existe:
    → procesar, marcar como procesado, responder 200 OK.
```

Procesar el mismo evento N veces debe producir **el mismo resultado** que procesarlo
una vez. Nunca doble confirmación, nunca doble liquidación. La restricción de unicidad
en base de datos sobre `(proveedor, idExternoEvento)` es la última línea de defensa.

### 4. Responder rápido, procesar seguro

Responder `200 OK` al proveedor apenas el evento esté **guardado de forma segura**. Si
el procesamiento es pesado, se puede hacer asíncrono, pero la recepción/persistencia
del evento debe ser inmediata para que el proveedor no reintente en exceso.

### 5. Reconciliación de respaldo

Los webhooks se pierden (caídas de red, timeouts). El job `ReconciliarOrdenesUseCase`
consulta periódicamente al proveedor el estado de las órdenes atascadas y las lleva al
estado correcto. **El sistema converge aunque un webhook nunca llegue.**

### Caminos tristes a cubrir siempre

- Firma inválida → registrar, rechazar.
- Evento duplicado → no-op idempotente.
- Orden inexistente para la referencia → `EN_REVISION`.
- Pago con monto distinto al esperado → `EN_REVISION` / `FALLIDA`.
- Pago que llega **después** de que la orden expiró → decisión de negocio explícita
  (reembolso vía proveedor / revisión manual).
- Webhook fuera de orden (llega "convertido" antes que "detectado") → tolerar con la
  máquina de estados.

---

## Seguridad de credenciales

- Todas las API keys, secrets y URLs de proveedores → **variables de entorno** o gestor
  de secretos. Nunca en el código, nunca en el repositorio, nunca en logs.
- Endpoints de webhook expuestos con validación de firma; considerar además lista
  blanca de IPs si el proveedor la ofrece.
- Rotación de credenciales prevista (no hardcodear de forma que rotar sea un dolor).

---

## Configuración por proveedor

Cada adaptador lee su configuración de forma aislada (perfil/propiedades), de modo que
activar/desactivar un proveedor o cambiar de sandbox a producción sea configuración, no
código:

```
pasarela.proveedores.binance.api-key      = ${BINANCE_API_KEY}
pasarela.proveedores.binance.secret       = ${BINANCE_SECRET}
pasarela.proveedores.binance.base-url     = ${BINANCE_BASE_URL}   # sandbox o prod
pasarela.proveedores.binance.habilitado   = true
```
