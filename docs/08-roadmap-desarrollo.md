# 08 — Roadmap de Desarrollo

> Ruta de construcción del MVP por **incrementos pequeños y verificables**. Cada
> incremento deja el sistema funcionando y probado antes de pasar al siguiente.

---

## Fase 0 — Fundaciones del proyecto

Dejar el esqueleto listo para construir sobre él.

- [ ] Proyecto Spring Boot 3 + Maven (ya generado con Spring Initializr).
- [ ] `pom.xml` con dependencias: Web, Data JPA, PostgreSQL, Flyway, Validation,
      Actuator, Lombok; y de test: JUnit 5, Mockito, Testcontainers, ArchUnit.
- [ ] Estructura de paquetes hexagonal (`compartido`, `pagos`, `comercios`,
      `liquidaciones`, cada uno con `dominio`/`aplicacion`/`infraestructura`).
- [ ] `compose.yaml` con PostgreSQL para desarrollo local.
- [ ] Configuración de conexión a base de datos (perfiles local/test).
- [ ] Primera migración Flyway (esquema base vacío o tabla de control).
- [ ] Prueba de arquitectura con ArchUnit (aunque el dominio esté casi vacío).
- [ ] Healthcheck de Actuator respondiendo.
- [ ] CI básico (GitHub Actions): `./mvnw verify` en cada push.

**Criterio de fin de fase:** `./mvnw verify` en verde, la app levanta, la BD conecta.

---

## Fase 1 — Núcleo de dominio: la Orden de Pago

El corazón, en Java puro, sin infraestructura. Todo con TDD.

- [ ] Value Objects: `Dinero`, `Moneda`, `Porcentaje`, `ReferenciaPago`, `IdOrden`.
- [ ] Enum `EstadoOrden` con las transiciones válidas.
- [ ] Entidad `OrdenDePago` con su máquina de estados y métodos de dominio
      (`confirmarPago`, `expirar`, `marcarComo...`, `estaExpirada`, `puedeConfirmarse`).
- [ ] `Comision` y cálculo de comisión.
- [ ] Excepciones de dominio (`OrdenNoPuedeConfirmarseException`, etc.).
- [ ] Pruebas unitarias exhaustivas: todas las transiciones válidas e inválidas.

**Criterio de fin de fase:** el dominio de pagos compila y está 100% probado sin Spring.

---

## Fase 2 — Persistencia de la Orden

Conectar el dominio a PostgreSQL sin ensuciarlo.

- [ ] Puerto `OrdenDePagoRepositorio` (en dominio).
- [ ] Entidad JPA `OrdenJpaEntity` (en infraestructura, separada del dominio).
- [ ] Mapper dominio ↔ JPA.
- [ ] Adaptador `OrdenRepositorioJpa` que implementa el puerto.
- [ ] Migración Flyway con la tabla de órdenes.
- [ ] Pruebas de integración con Testcontainers (guardar/recuperar, mapeo correcto).

**Criterio de fin de fase:** se persiste y recupera una orden contra un PostgreSQL real.

---

## Fase 3 — Comercios y onboarding

- [ ] Dominio `Comercio` (VOs `Nit`, `CuentaLiquidacion`, `LimitesOperacion`; estados de
      verificación; reglas `puedeCobrar`, `validarLimite`).
- [ ] Casos de uso: `RegistrarComercioUseCase`, `VerificarComercioUseCase`,
      `ConsultarComercioUseCase`.
- [ ] Persistencia del comercio (puerto + JPA + migración + tests).
- [ ] Adaptador REST: endpoints de registro y consulta.
- [ ] Panel/endpoint admin para verificación manual.
- [ ] Seguridad: Spring Security + JWT, roles `ADMIN` y `COMERCIO`.

**Criterio de fin de fase:** un comercio se registra, un admin lo verifica, con auth.

---

## Fase 4 — Crear orden + generación de cobro (con proveedor simulado)

- [ ] Puerto `ProveedorDePagoPort`.
- [ ] **Adaptador falso/stub** del proveedor (para avanzar sin depender del sandbox real).
- [ ] Caso de uso `CrearOrdenUseCase` (valida comercio, crea orden, pide cobro, persiste).
- [ ] Adaptador REST: endpoint de creación de orden que devuelve datos del QR.
- [ ] Página de pago mínima (o contrato de API para que el frontend la construya).
- [ ] Pruebas del caso de uso con puertos falsos + prueba de API.

**Criterio de fin de fase:** un comercio crea un cobro y obtiene un QR (con proveedor
simulado).

---

## Fase 5 — Webhook e idempotencia (el corazón crítico)

- [ ] Entidad `EventoProveedor` + su persistencia (con unicidad
      `(proveedor, idExternoEvento)`).
- [ ] Caso de uso `ProcesarWebhookPagoUseCase` (validar firma, guardar crudo,
      idempotencia, confirmar orden, notificar).
- [ ] Adaptador REST: endpoint de webhook.
- [ ] `NotificadorPort` + implementación (para el "PAGADO ✓" en tiempo real).
- [ ] Pruebas: firma válida/ inválida, **idempotencia (mismo evento dos veces)**,
      orden inexistente, pago tardío, monto errado.

**Criterio de fin de fase:** un webhook confirma la orden de forma idempotente y robusta.

---

## Fase 6 — Jobs de expiración y reconciliación

- [ ] `ExpirarOrdenesUseCase` + job programado.
- [ ] `ReconciliarOrdenesUseCase` + job programado (consulta al proveedor).
- [ ] Pruebas de ambos jobs.

**Criterio de fin de fase:** el sistema converge al estado correcto aun sin webhooks.

---

## Fase 7 — Liquidación y conciliación

- [ ] Dominio `Liquidacion` (registrar, conciliar, discrepancias).
- [ ] Casos de uso `RegistrarLiquidacionUseCase`, `ConciliarLiquidacionUseCase`.
- [ ] Cálculo y registro de la comisión de la plataforma.
- [ ] Persistencia y pruebas.

**Criterio de fin de fase:** cada liquidación queda registrada y conciliada, con comisión.

---

## Fase 8 — Dashboard del comercio

- [ ] `ConsultarVentasUseCase` (ventas del día/mes).
- [ ] `ExportarMovimientosUseCase` (CSV/Excel).
- [ ] Comprobante por transacción.
- [ ] Endpoints REST para el frontend.

**Criterio de fin de fase:** el comercio ve sus ventas y exporta su historial.

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
