# 08 — Roadmap de Desarrollo

> Ruta de construcción del MVP por **incrementos pequeños y verificables**. Cada
> incremento deja el sistema funcionando y probado antes de pasar al siguiente.

---

## Fase 0 — Fundaciones del proyecto

Dejar el esqueleto listo para construir sobre él.

- [x] Proyecto Spring Boot 3 + Maven (ya generado con Spring Initializr).
- [x] `pom.xml` con dependencias: Web, Data JPA, PostgreSQL, Flyway, Validation,
      Actuator; y de test: JUnit 5, Mockito, Testcontainers, ArchUnit.
- [x] Estructura de paquetes hexagonal (`compartido`, `pagos`, `comercios`,
      `liquidaciones`, cada uno con `dominio`/`aplicacion`/`infraestructura`).
- [x] `compose.yaml` con PostgreSQL para desarrollo local.
- [x] Configuración de conexión a base de datos (perfiles local/test).
- [x] Primera migración Flyway (esquema base vacío o tabla de control).
- [x] Prueba de arquitectura con ArchUnit (aunque el dominio esté casi vacío).
- [x] Healthcheck de Actuator respondiendo.
- [x] CI básico (GitHub Actions): `./mvnw verify` en cada push.

**Criterio de fin de fase:** `./mvnw verify` en verde, la app levanta, la BD conecta.
**Cerrada:** `v0.0.1`.

---

## Fase 1 — Núcleo de dominio: la Orden de Pago

El corazón, en Java puro, sin infraestructura. Todo con TDD.

- [x] Value Objects: `Dinero`, `Moneda`, `Porcentaje`, `ReferenciaPago`, `IdOrden`.
- [x] Enum `EstadoOrden` con las transiciones válidas.
- [x] Entidad `OrdenDePago` con su máquina de estados y métodos de dominio
      (`confirmarPago`, `expirar`, `marcarComo...`, `estaExpirada`, `puedeConfirmarse`).
- [x] `Comision` y cálculo de comisión.
- [x] Excepciones de dominio (`OrdenNoPuedeConfirmarseException`, etc.).
- [x] Pruebas unitarias exhaustivas: todas las transiciones válidas e inválidas
      (producto cartesiano estado × evento) + PIT al 100%.

**Criterio de fin de fase:** el dominio de pagos compila y está 100% probado sin Spring.
**Cerrada:** `v0.1.0`.

---

## Fase 2 — Persistencia de la Orden

Conectar el dominio a PostgreSQL sin ensuciarlo.

- [x] Puerto `OrdenDePagoRepositorio` (en dominio).
- [x] Entidad JPA `OrdenJpaEntity` (en infraestructura, separada del dominio).
- [x] Mapper dominio ↔ JPA.
- [x] Adaptador `OrdenDePagoRepositorioJpa` que implementa el puerto.
- [x] Migración Flyway con la tabla de órdenes.
- [x] Pruebas de integración con Testcontainers (guardar/recuperar, mapeo correcto).

**Criterio de fin de fase:** se persiste y recupera una orden contra un PostgreSQL real.
**Cerrada:** `v0.1.0`.

---

## Fase 3 — Comercios y onboarding

- [x] Dominio `Comercio` (VOs `Nit`, `CuentaLiquidacion`, `LimitesOperacion`; estados de
      verificación; reglas `puedeCobrar`, `validarCobro`).
- [x] Casos de uso: `RegistrarComercioUseCase`, `DecidirVerificacionUseCase`,
      `ConsultarComercioUseCase`, `ActualizarLimitesUseCase`.
- [x] Persistencia del comercio (puerto + JPA + migración + tests).
- [x] Adaptador REST: endpoints de registro y consulta.
- [x] Endpoint admin para verificación manual (aprobar/rechazar/suspender/reactivar).
- [x] Seguridad: contexto `seguridad` con JWT propio (Nimbus), roles `ADMIN` y `COMERCIO`.

**Criterio de fin de fase:** un comercio se registra, un admin lo verifica, con auth.
**Cerrada:** `v0.2.0`.

---

## Fase 4 — Crear orden + generación de cobro (con proveedor simulado)

- [x] Puerto `ProveedorDePagoPort`.
- [x] **Adaptador simulado** del proveedor (`ProveedorDePagoSimulado`, con fallos
      configurables y gateado por propiedad — nunca activo en prod).
- [x] Caso de uso `CrearOrdenUseCase` (valida comercio, crea orden, pide cobro, persiste).
- [x] Adaptador REST: endpoint de creación de orden que devuelve datos del QR.
- [x] Contrato de API documentado con springdoc-openapi para el frontend Angular.
- [x] Pruebas del caso de uso con puertos falsos + prueba de API + resiliencia
      (proveedor caído → 502, cero órdenes fantasma).

**Criterio de fin de fase:** un comercio crea un cobro y obtiene un QR (con proveedor
simulado).
**Cerrada:** `v0.3.0`.

---

## Fase 5 — Webhook e idempotencia (el corazón crítico)

- [x] Entidad `EventoProveedor` + su persistencia (con unicidad
      `(proveedor, idExternoEvento)`, verificada a nivel SQL).
- [x] Caso de uso `ProcesarWebhookUseCase` (validar firma, guardar crudo,
      idempotencia, confirmar orden, notificar).
- [x] Adaptador REST: endpoint de webhook.
- [x] `NotificadorDeComercios` + implementación (ver ADR-005: polling corto sobre
      `GET /api/ordenes/{id}` y `GET /api/pagos/{referencia}`, no push en tiempo real).
- [x] Pruebas: firma válida/inválida, **idempotencia bajo concurrencia real**
      (8 hilos contra PostgreSQL), orden inexistente, pago tardío, monto errado,
      webhook fuera de orden.

**Criterio de fin de fase:** un webhook confirma la orden de forma idempotente y robusta.
**Cerrada:** `v0.4.0`.

---

## Fase 6 — Jobs de expiración y reconciliación

- [x] `ExpirarOrdenesVencidasUseCase` + job programado (bloqueo optimista para la
      carrera expiración-vs-pago, probada con hilos contra PostgreSQL).
- [x] `ReconciliarOrdenesUseCase` + job programado (consulta al proveedor y confirma
      por la MISMA ruta idempotente del webhook — cierra ADR-004).
- [x] Pruebas de ambos jobs.

**Criterio de fin de fase:** el sistema converge al estado correcto aun sin webhooks.
**Cerrada:** `v0.5.0`.

---

## Fase 7 — Liquidación y conciliación

- [x] Dominio `Liquidacion` (registrar, conciliar, discrepancias con detalle).
- [x] Casos de uso `RegistrarLiquidacionUseCase`, `ConciliarLiquidacionUseCase`.
- [x] Cálculo y registro de la comisión de la plataforma (bruto − comisión = neto,
      al centavo).
- [x] Persistencia y pruebas.

**Criterio de fin de fase:** cada liquidación queda registrada y conciliada, con comisión.
**Cerrada:** `v0.5.0`.

---

## Fase 8 — Dashboard del comercio

- [x] `ConsultarVentasUseCase` (ventas del día/mes, contexto **Pagos** — HU-018).
- [ ] `ExportarVentasUseCase` (CSV contador-ready — HU-019, 🔵 en curso).
- [ ] Comprobante por transacción (HU-020).
- [x] Endpoints REST para el frontend (`/api/ventas/*`, ampliándose con HU-019/020).

**Criterio de fin de fase:** el comercio ve sus ventas y exporta su historial.
**Sprint 6 en curso** (sin tag todavía).

---

## Fase 9 — Integración real con Binance Pay (sandbox → producción)

> Requiere el gatillo legal/KYB (ver README y ADR-001). El desarrollo previo usó el
> adaptador simulado; ahora se reemplaza por el real.

- [ ] `BinancePayAdapter` real (crear cobro, validar firma, interpretar webhook).
- [ ] Configuración de credenciales por variables de entorno.
- [ ] Pruebas de integración contra el **sandbox** de Binance.
- [ ] Endurecimiento: límites por transacción, auditoría, rate limiting, manejo de todos
      los caminos tristes end-to-end.

**Criterio de fin de fase:** pago real de extremo a extremo en sandbox.

---

## Fase 10 — Piloto

- [ ] Paso a producción con el proveedor (KYB aprobado).
- [ ] Observabilidad: logs estructurados + alertas (webhooks fallidos, órdenes atascadas).
- [ ] 5-10 comercios reales; primeras transacciones controladas.
- [ ] Medición de los criterios de éxito del MVP (ver doc de Alcance).

**Criterio de fin de fase:** MVP validado con comercios reales.

---

## Post-MVP (fuera de este roadmap)

- Riel on-chain (segundo adaptador de `ProveedorDePagoPort`).
- Segundo proveedor de rampa (redundancia).
- App móvil del comercio.
- Directorio público de comercios.
- Módulo P2P.

---

## Principios del roadmap

- **Vertical antes que horizontal:** preferir una funcionalidad completa de punta a
  punta (aunque simple) sobre muchas piezas a medias.
- **Proveedor simulado primero:** no bloquear el desarrollo esperando accesos externos;
  el `ProveedorDePagoPort` con un stub permite construir casi todo el MVP sin el sandbox.
- **Cada fase deja verde `./mvnw verify`** y el sistema en un estado usable.
- **Lo legal corre en paralelo** y se cruza con lo técnico en la Fase 9-10, no antes.
