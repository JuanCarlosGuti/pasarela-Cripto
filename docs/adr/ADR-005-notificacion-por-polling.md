# ADR-005 — Notificación "PAGADO ✓" por polling corto (no SSE)

**Estado:** Aceptado

## Contexto

Cuando una orden pasa a `PAGO_DETECTADO`, el comercio debe ver "PAGADO ✓" en su
pantalla en segundos (HU-013). Las opciones evaluadas para llevar el cambio de
estado al frontend fueron **SSE** (Server-Sent Events), **WebSockets** y
**polling corto** sobre los endpoints de consulta existentes.

La fuente de verdad ya existe y está probada: `GET /api/ordenes/{id}` (la caja
del comercio, autenticada) y `GET /api/pagos/{referencia}` (la página del
pagador, pública) devuelven el estado actual de la orden.

## Decisión

**Polling corto (2-3 segundos) sobre los endpoints de consulta existentes.**

- La caja del comercio consulta `GET /api/ordenes/{id}` hasta ver
  `PAGO_DETECTADO`.
- La página de pago del pagador consulta `GET /api/pagos/{referencia}`.
- Ambas respuestas viajan con `Cache-Control: no-store`: ningún proxy o caché
  intermedio puede servir un estado viejo a un cliente que está esperando
  su confirmación.
- El puerto `NotificadorDeComercios` se mantiene como punto de extensión
  (hoy su adaptador deja el evento en el log). Si el piloto exige empuje real
  (SSE, push, correo), se agrega OTRO adaptador del mismo puerto sin tocar
  dominio ni aplicación — el mismo patrón de ADR-003.

## Consecuencias

**A favor:**
- Cero infraestructura nueva: sin conexiones persistentes, sin heartbeats,
  sin reconexión, sin configuración especial de proxies/balanceadores.
- Los endpoints ya tienen la matriz de seguridad y aislamiento probada.
- Con la ventana de pago de 15 minutos y polling de 2-3 s, el costo es
  irrelevante (< 450 requests por cobro en el peor caso, respuestas de ~100
  bytes).
- El frontend Angular lo implementa con un `interval` trivial.

**En contra:**
- Latencia media de confirmación ≈ la mitad del intervalo de polling
  (~1.5 s) — aceptable frente al criterio "en segundos".
- Tráfico repetitivo que SSE evitaría; irrelevante a la escala del piloto
  (decenas de comercios). Si duele, la salida ya está diseñada (nuevo
  adaptador del puerto).

## Notas

La confirmación de la orden **jamás depende de la notificación**: es
best-effort por diseño y está probado que su fallo no revierte nada (HU-010).
La fuente de verdad es el estado de la orden en la base de datos.
