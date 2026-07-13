# Gestión 02 — Backlog de Historias de Usuario

> Backlog del MVP, derivado de `docs/01` (alcance), `docs/04` (dominio), `docs/05`
> (casos de uso) y `docs/08` (roadmap). **Este archivo es la fuente de verdad del
> backlog**: el estado se actualiza aquí, versionado en git.
>
> - **HU-xxx** = historia de usuario (valor de negocio visible).
> - **T-xxx** = tarea técnica habilitadora (sin actor de negocio directo).
> - Cada historia se implementa en una rama `feature/HU-xxx-...` (ver
>   `01-flujo-de-trabajo-git.md`) y cumple la **Definition of Done** de
>   `docs/03-guia-estilo-clean-code.md`.

**Roles:** `Comercio` · `Admin` (operador de la plataforma) · `Pagador` ·
`Sistema` (webhooks/jobs) · `Dev` (tareas técnicas).

**Estados:** ⬜ pendiente · 🔵 en curso · ✅ terminada

---

## Épica E0 — Fundaciones técnicas *(Sprint 0 / Fase 0)*

### ✅ T-001 — Esqueleto Spring Boot con estructura hexagonal
**Como** Dev **quiero** el proyecto Spring Boot 3 con la estructura de paquetes
hexagonal **para** construir todo lo demás sobre fronteras verificadas.

**Criterios de aceptación:**
- **Dado** el repositorio clonado, **cuando** ejecuto `./mvnw verify`, **entonces**
  compila en verde con Java 25+ y el wrapper de Maven incluido.
- **Dado** el árbol de paquetes, **entonces** existen `compartido`, `pagos`,
  `comercios`, `liquidaciones`, cada uno con `dominio/`, `aplicacion/`,
  `infraestructura/`, y `PagosApplication.java` es el único `@SpringBootApplication`.
- El `pom.xml` incluye: Web, Data JPA, PostgreSQL, Flyway, Validation, Actuator y
  (test) JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit, JaCoCo.
- Se elimina la clase `Main.java` de la plantilla de IntelliJ.

### ✅ T-002 — Base de datos local con Docker Compose + Flyway
**Como** Dev **quiero** PostgreSQL levantado con `docker compose up -d` y migraciones
Flyway **para** que el entorno local sea reproducible en un comando.

**Criterios de aceptación:**
- **Dado** Docker corriendo, **cuando** ejecuto `docker compose up -d` y arranco la
  app, **entonces** conecta a PostgreSQL y Flyway aplica `V1__esquema_base.sql`.
- Perfiles `local` y `test` separados; credenciales por variables de entorno con
  defaults inocuos para local.
- `GET /actuator/health` responde `UP` (incluye estado de la BD).

### ✅ T-003 — Guardián de arquitectura (ArchUnit)
**Como** Dev **quiero** pruebas ArchUnit desde el día cero **para** que ninguna
violación de fronteras sobreviva a un `verify`.

**Criterios de aceptación:**
- Reglas activas: `dominio` no depende de Spring ni JPA; `aplicacion` no depende de
  `infraestructura`; los contextos no se importan entre sí (solo vía `compartido`).
- **Dado** una clase de dominio que importe `jakarta.persistence`, **cuando** corre
  `verify`, **entonces** la build **falla**.

### ✅ T-004 — CI: verify en cada push
**Como** Dev **quiero** GitHub Actions ejecutando `./mvnw verify` (con Testcontainers)
en cada push **para** que `develop` y `main` no puedan degradarse sin aviso.

**Criterios de aceptación:**
- Workflow corre en push a `main`, `develop` y `feature/**`; falla si fallan tests,
  ArchUnit o la cobertura configurada.
- Badge de estado en el README.
- Reglas de protección de rama activas en `main` (status check requerido, sin force
  push) y `develop` (sin force push).

---

## Épica E1 — Núcleo de dominio: Orden de Pago *(Sprint 1 / Fase 1)*

### ✅ HU-001 — Value Objects del dinero
**Como** Dev (en nombre del negocio) **quiero** VOs `Dinero`, `Moneda`, `Porcentaje`
**para** que ningún monto viaje como primitivo sin validar.

**Criterios de aceptación:**
- **Dado** un monto negativo, **cuando** creo `Dinero`, **entonces** lanza excepción
  de dominio.
- `Dinero` no permite operar entre monedas distintas (sumar COP + USDT falla).
- `porcentaje(2.5)` sobre `Dinero(40000 COP)` devuelve `Dinero(1000 COP)`; redondeo
  documentado y probado (banker's rounding o el que se decida — dejarlo explícito).
- Son `record`s inmutables, sin Spring/JPA (ArchUnit lo garantiza).

### ✅ HU-002 — Máquina de estados de la Orden de Pago
**Como** Comercio **quiero** que una orden solo pueda avanzar por transiciones
válidas **para** que jamás exista un cobro en estado inconsistente.

**Criterios de aceptación (matriz completa de `docs/04`):**
- **Dado** `CREADA`, **cuando** se registra el cobro en el proveedor, **entonces**
  pasa a `PENDIENTE_PAGO`.
- **Dado** `PENDIENTE_PAGO` no expirada, **cuando** `confirmarPago(evento)`,
  **entonces** pasa a `PAGO_DETECTADO` y guarda el timestamp de la transición.
- **Dado** `PENDIENTE_PAGO` vencida, **cuando** `expirar()`, **entonces** `EXPIRADA`.
- **Dado** cualquier estado terminal (`LIQUIDADA`, `EXPIRADA`, `EN_REVISION`),
  **cuando** se intenta cualquier transición, **entonces** lanza
  `OrdenNoPuedeConfirmarseException` (o la excepción específica) y **no** cambia nada.
- **Toda transición no listada en la matriz de `docs/04` se rechaza** — la prueba
  recorre el producto cartesiano estado × evento.
- No existe `setEstado` público; el historial de transiciones queda registrado.

### ✅ HU-003 — Expiración y comisión de la orden
**Como** Plataforma **quiero** calcular la expiración y la comisión de cada orden
**para** proteger el cobro y registrar mi ingreso.

**Criterios de aceptación:**
- **Dado** una orden creada con ventana de 15 min, **cuando** consulto
  `estaExpirada(ahora)` un segundo después del límite, **entonces** `true`; un
  segundo antes, `false` (borde probado).
- **Dado** una comisión de 2.5% sobre $40.000, **entonces** `Comision.calcular`
  devuelve $1.000 y el neto del comercio $39.000.
- El reloj se **inyecta** (`Clock`/`Instant` como parámetro): ninguna regla usa
  `Instant.now()` directo — requisito de testeabilidad.

---

## Épica E2 — Persistencia de la Orden *(Sprint 1 / Fase 2)*

### ✅ T-005 — Adaptador JPA de OrdenDePago
**Como** Dev **quiero** persistir y recuperar órdenes vía el puerto
`OrdenDePagoRepositorio` **para** conectar el dominio a PostgreSQL sin contaminarlo.

**Criterios de aceptación:**
- Entidad `OrdenJpaEntity` separada del dominio + mapper bidireccional; migración
  Flyway `V2__ordenes.sql` con índice único sobre la referencia de pago.
- **Dado** una orden con historial de transiciones, **cuando** la guardo y recupero
  (Testcontainers + PostgreSQL real), **entonces** todos los campos y el historial
  sobreviven el viaje completo (ida y vuelta sin pérdidas).
- `buscarPorReferencia` y `buscarPendientesExpiradas(ahora)` probados con datos que
  incluyen falsos positivos (órdenes que NO deben aparecer).

---

## Épica E3 — Comercios, onboarding y seguridad *(Sprint 2 / Fase 3)*

### ✅ HU-004 — Registro de comercio
**Como** dueño de un Comercio **quiero** registrarme con los datos de mi negocio y
mi cuenta de liquidación **para** empezar el proceso de verificación.

**Criterios de aceptación:**
- **Dado** datos válidos (razón social, NIT, cuenta de liquidación), **cuando**
  `POST /api/comercios`, **entonces** 201, el comercio queda `PENDIENTE` y no puede
  cobrar todavía.
- **Dado** un NIT con dígito de verificación inválido, **entonces** 400 con mensaje
  claro (el VO `Nit` valida el algoritmo DIAN — probado con NITs reales válidos e
  inválidos).
- **Dado** un NIT ya registrado, **entonces** 409 (sin duplicados).
- Ningún dato sensible aparece en logs ni en la URL.

### ✅ HU-005 — Verificación manual del comercio
**Como** Admin **quiero** aprobar, rechazar o suspender comercios **para** controlar
quién puede cobrar por la plataforma.

**Criterios de aceptación:**
- **Dado** un comercio `PENDIENTE`, **cuando** el Admin lo aprueba, **entonces**
  pasa a `VERIFICADO` y `puedeCobrar()` es `true`.
- **Dado** un rechazo o suspensión, **entonces** requiere `motivo` (auditable) y
  `puedeCobrar()` es `false`.
- **Dado** un usuario con rol `COMERCIO`, **cuando** intenta verificar un comercio,
  **entonces** 403 (solo `ADMIN`).
- Las transiciones de verificación inválidas (p. ej. verificar un `SUSPENDIDO` sin
  reactivación explícita) se rechazan en el dominio.

### ✅ HU-006 — Autenticación y roles
**Como** Plataforma **quiero** autenticación JWT con roles `ADMIN` y `COMERCIO`
**para** que cada quien acceda solo a lo suyo.

**Criterios de aceptación:**
- **Dado** credenciales válidas, **cuando** `POST /api/auth/login`, **entonces**
  devuelve JWT con expiración; con credenciales inválidas, 401 **sin revelar** si el
  usuario existe.
- **Dado** un token expirado o manipulado, **entonces** 401 en cualquier endpoint
  protegido.
- **Dado** un comercio A autenticado, **cuando** consulta recursos del comercio B,
  **entonces** 403/404 (aislamiento por dueño probado explícitamente).
- El secreto JWT viene de variable de entorno; contraseñas con BCrypt; sin datos
  sensibles en el token.

### ✅ HU-007 — Límites de operación del comercio
**Como** Admin **quiero** topes por transacción y por mes para cada comercio
**para** cumplir los controles del MVP desde el día uno.

**Criterios de aceptación:**
- **Dado** un tope por transacción de $2.000.000, **cuando** el comercio intenta
  cobrar $2.000.001, **entonces** el dominio rechaza con excepción clara.
- **Dado** un tope mensual casi consumido, **cuando** el cobro lo excedería,
  **entonces** se rechaza y queda registro en la bitácora de operaciones inusuales.
- Solo `ADMIN` puede modificar límites; el cambio queda auditado (quién, cuándo,
  valor anterior/nuevo).

---

## Épica E4 — Crear cobro con QR *(Sprint 3 / Fase 4)*

### ✅ HU-008 — Crear orden de pago con QR
**Como** Comercio verificado **quiero** digitar un monto y obtener un QR con
expiración **para** cobrarle a un cliente cripto en segundos.

**Criterios de aceptación:**
- **Dado** un comercio `VERIFICADO` autenticado, **cuando**
  `POST /api/ordenes {monto: 40000}`, **entonces** 201 con datos del QR (contenido,
  deeplink), referencia única y `expiraEn`; la orden queda `PENDIENTE_PAGO`.
- **Dado** un comercio `PENDIENTE` o `SUSPENDIDO`, **entonces** 403 y no se crea nada.
- **Dado** un monto que viola límites (HU-007) o ≤ 0, **entonces** 400/422 y no se
  llama al proveedor.
- **Dado** que el proveedor falla al crear el cobro, **entonces** la orden **no**
  queda `PENDIENTE_PAGO` fantasma (o no se persiste, o queda `FALLIDA` con motivo —
  decisión documentada) y el comercio recibe un error accionable.
- La `ReferenciaPago` es única (colisión probada) y no contiene datos del comercio.

### ✅ T-006 — Adaptador simulado del proveedor
**Como** Dev **quiero** un `ProveedorDePagoSimulado` que implemente
`ProveedorDePagoPort` **para** desarrollar todo el MVP sin depender del sandbox de
Binance.

**Criterios de aceptación:**
- Implementa el puerto completo: crear cobro (QR fake determinista), validar firma
  (secreto compartido de prueba), interpretar webhook.
  > *Nota de alcance (T-006):* el puerto solo define `crearCobro` en el Sprint 3;
  > la validación de firma y la interpretación de webhooks entran al puerto con
  > HU-010 (Sprint 4) y el simulador las implementará en ese momento.
- Puede **simular fallos** configurables (timeout, error 500, firma inválida) para
  los tests de resiliencia.
- Se activa por perfil/propiedad (`pasarela.proveedores.simulado.habilitado=true`);
  jamás activo en producción (probado: con perfil `prod`, el bean no existe).

### ✅ HU-009 — Consultar estado de la orden
**Como** Comercio **quiero** consultar el estado de mis órdenes (y el Pagador el de
la suya, por referencia pública) **para** saber en qué va cada cobro.

**Criterios de aceptación:**
- **Dado** el dueño autenticado, **cuando** `GET /api/ordenes/{id}`, **entonces**
  devuelve estado, monto, timestamps de transición.
- **Dado** otro comercio, **entonces** 404 (no filtrar existencia).
- Endpoint público de consulta por referencia (para la página de pago) expone
  **solo** estado y monto — nada del comercio ni interno (contrato probado).

---

## Épica E5 — Webhook e idempotencia *(Sprint 4 / Fase 5 — LA CRÍTICA)*

### ✅ HU-010 — Confirmación de pago por webhook
**Como** Sistema **quiero** procesar la notificación de pago del proveedor
**para** que el comercio vea "PAGADO ✓" en segundos.

**Criterios de aceptación (camino feliz):**
- **Dado** una orden `PENDIENTE_PAGO` y un webhook con firma válida, **cuando**
  llega `POST /api/webhooks/{proveedor}`, **entonces**: el evento crudo queda
  guardado ANTES de procesar → la orden pasa a `PAGO_DETECTADO` → se notifica al
  comercio → responde 200 en < 2s.
- El orden de los pasos es **exactamente** el del flujo de `docs/05` (firma → crudo
  → idempotencia → interpretar → confirmar → notificar) y hay un test que lo
  demuestra.

### ✅ HU-011 — Idempotencia estricta del webhook
**Como** Plataforma **quiero** que procesar el mismo evento N veces produzca
exactamente el mismo resultado que una vez **para** que el doble cobro sea
**imposible** (ADR-004).

**Criterios de aceptación:**
- **Dado** un webhook ya procesado, **cuando** llega de nuevo (mismo
  `(proveedor, idExternoEvento)`), **entonces** responde 200, **no** cambia la orden,
  **no** re-notifica, **no** duplica registros.
- La unicidad `(proveedor, idExternoEvento)` existe como **constraint en BD**
  (migración probada: el insert duplicado falla a nivel SQL).
- **Prueba de concurrencia:** el mismo evento llegando **en paralelo** (2+ hilos
  simultáneos contra PostgreSQL real vía Testcontainers) produce una sola
  confirmación — la constraint es la última línea de defensa y se ejercita de verdad.

### ✅ HU-012 — Caminos tristes del webhook
**Como** Plataforma **quiero** manejar explícitamente cada webhook anómalo
**para** que ningún caso raro deje una orden inconsistente ni pase inadvertido.

**Criterios de aceptación (uno por camino triste de `docs/07`):**
- **Firma inválida** → se registra el intento, responde 401, la orden no cambia.
- **Orden inexistente** para la referencia → evento guardado y marcado para
  revisión; alerta en bitácora; 200 (no provocar reintentos infinitos del proveedor).
- **Monto distinto** al esperado → orden a `EN_REVISION`, bitácora, notificación al
  Admin.
- **Pago tardío** (orden ya `EXPIRADA`) → NO se confirma; va a revisión manual con
  todos los datos para gestionar el reembolso vía proveedor.
- **Webhook fuera de orden** (p. ej. "convertida" antes que "detectada") → la
  máquina de estados lo tolera según la matriz; nada se corrompe.
- Cada caso tiene su **test de API de extremo a extremo** (no solo unitario).

### ✅ HU-013 — Notificación "PAGADO ✓" al comercio
**Como** Comercio **quiero** ver la confirmación en mi pantalla en tiempo real
**para** entregar el producto con confianza.

**Criterios de aceptación:**
- **Dado** una orden que pasa a `PAGO_DETECTADO`, **entonces** el `NotificadorDeComercios`
  registra el evento (best-effort); el frontend se entera por **polling corto**
  (decisión tomada y registrada en [ADR-005](../adr/ADR-005-notificacion-por-polling.md)).
- Si la notificación falla, la confirmación de la orden **no** se revierte (la
  notificación es best-effort; la fuente de verdad es el estado).

---

## Épica E6 — Convergencia: jobs de respaldo *(Sprint 5 / Fase 6)*

### ✅ HU-014 — Expiración automática de órdenes
**Como** Sistema **quiero** expirar las órdenes vencidas sin pago **para** que no
queden cobros zombis pendientes para siempre.

**Criterios de aceptación:**
- **Dado** órdenes `PENDIENTE_PAGO` vencidas, **cuando** corre el job, **entonces**
  pasan a `EXPIRADA`; las no vencidas no se tocan (borde exacto probado).
- El job es idempotente (correr dos veces seguidas = mismo resultado) y procesa por
  lotes (no carga toda la tabla en memoria).
- **Dado** que una orden recibe su pago justo mientras el job corre (carrera
  expiración vs. confirmación), **entonces** solo una de las dos transiciones gana y
  la otra se rechaza limpiamente — probado con Testcontainers.

### ✅ HU-015 — Reconciliación de órdenes atascadas
**Como** Plataforma **quiero** consultar activamente al proveedor por órdenes
atascadas **para** converger al estado correcto aunque un webhook nunca llegue
(ADR-004).

**Criterios de aceptación:**
- **Dado** una orden `PENDIENTE_PAGO` con más de X minutos (configurable),
  **cuando** corre el job y el proveedor reporta "pagada", **entonces** la orden se
  confirma por la **misma ruta idempotente** que el webhook (sin duplicar lógica).
- **Dado** que el proveedor no responde, **entonces** el job registra el fallo y
  reintenta en el siguiente ciclo (no se cae ni bloquea las demás órdenes).
- **Dado** que webhook y reconciliación procesan el mismo pago casi a la vez,
  **entonces** la idempotencia (HU-011) garantiza una sola confirmación.

---

## Épica E7 — Liquidación y conciliación *(Sprint 5 / Fase 7)*

### ✅ HU-016 — Registro de liquidación
**Como** Plataforma **quiero** registrar cada liquidación que el proveedor hace al
comercio **para** que cada peso quede trazado (invariante #4 del producto).

**Criterios de aceptación:**
- **Dado** órdenes `CONVERTIDA` liquidadas por el proveedor, **cuando** se registra
  la liquidación, **entonces** agrupa las órdenes, calcula bruto, comisión y neto, y
  las órdenes pasan a `LIQUIDADA`.
- Una orden no puede pertenecer a dos liquidaciones (probado).
- `montoBruto = Σ órdenes` y `neto = bruto − comisión` cuadran **al centavo**
  (propiedad verificada en tests con montos que fuerzan redondeos).

### ✅ HU-017 — Conciliación contra el proveedor
**Como** Admin **quiero** conciliar lo registrado contra lo reportado por el
proveedor **para** detectar cualquier discrepancia de dinero de inmediato.

**Criterios de aceptación:**
- **Dado** que los datos coinciden, **entonces** la liquidación pasa a `CONCILIADA`.
- **Dado** cualquier diferencia (monto, órdenes faltantes/sobrantes), **entonces**
  `DISCREPANCIA` + detalle de la diferencia + alerta al Admin; jamás se "cuadra"
  silenciosamente.

---

## Épica E8 — Dashboard del comercio *(Sprint 6 / Fase 8)*

### ✅ HU-018 — Ventas del día y del mes
**Como** Comercio **quiero** ver mis ventas del día y del mes **para** controlar mi
negocio sin pedirle nada a nadie.

**Criterios de aceptación:**
- **Dado** el comercio autenticado, **entonces** ve totales y listado del día/mes
  (solo SUS órdenes — aislamiento probado), con paginación.
- Los totales solo cuentan órdenes efectivamente pagadas/liquidadas (definición
  exacta documentada en el endpoint).

### 🔵 HU-019 — Exportar movimientos (contador-ready)
**Como** Comercio **quiero** exportar mi historial a CSV/Excel **para** entregárselo
a mi contador tal cual.

**Criterios de aceptación:**
- Export CSV con: fecha, referencia, monto bruto, comisión, neto, estado; filtro por
  rango de fechas; separador y encoding compatibles con Excel en español (`;` y BOM
  UTF-8 — probado abriendo el archivo real).
- **Dado** un rango sin movimientos, **entonces** archivo válido con solo encabezados.

### ⬜ HU-020 — Comprobante por transacción
**Como** Comercio **quiero** un comprobante de cada pago **para** soportar la venta
ante el cliente y el contador.

**Criterios de aceptación:**
- **Dado** una orden pagada/liquidada, **entonces** genera comprobante con datos del
  cobro, timestamps y referencia del proveedor.
- Órdenes no pagadas no generan comprobante (422).

---

## Épica E9 — Binance Pay real *(Sprint 7 / Fase 9)*

### ⬜ HU-021 — Adaptador real de Binance Pay
**Como** Plataforma **quiero** reemplazar el simulador por el adaptador real de
Binance Pay **para** procesar pagos verdaderos en sandbox.

**Criterios de aceptación:**
- `BinancePayAdapter` implementa el puerto completo: crear cobro (firma
  HMAC-SHA512), validar firma del webhook, interpretar payload.
- **El cambio simulador → real es SOLO configuración** (cero cambios en dominio y
  aplicación — verificado: el diff no toca esos paquetes). Valida ADR-003.
- Tests de contrato contra **WireMock** (respuestas reales grabadas del sandbox:
  éxito, error, timeout, payload inesperado) + suite manual contra sandbox real
  documentada en el repo.
- Credenciales solo por variables de entorno; los tests no contienen secretos
  reales.

### ⬜ HU-022 — Endurecimiento pre-producción
**Como** Plataforma **quiero** rate limiting, auditoría y revisión de seguridad
**para** exponer el sistema a internet con riesgo controlado.

**Criterios de aceptación:**
- Rate limiting en endpoints públicos (login, webhook, consulta pública) — probado.
- Todos los caminos tristes de HU-012 re-verificados **end-to-end contra el sandbox
  real**.
- Escaneo de dependencias (OWASP/Dependabot) sin vulnerabilidades críticas abiertas.
- Checklist de seguridad: sin secretos en el repo (verificado con gitleaks), sin
  datos sensibles en logs, headers de seguridad correctos.

---

## Épica E10 — Piloto *(Sprint 8 / Fase 10)*

### ⬜ HU-023 — Observabilidad para operar
**Como** Admin **quiero** logs estructurados, métricas y alertas **para** enterarme
de un problema antes que el comercio.

**Criterios de aceptación:**
- Logs JSON estructurados con ID de correlación por request/orden.
- Alertas mínimas: webhook fallido repetido, órdenes atascadas > umbral,
  discrepancia de conciliación, health caído.
- Panel simple con: órdenes/día, tasa de éxito, tiempo QR→confirmación (las métricas
  del criterio de éxito de `docs/01`).

### ⬜ HU-024 — Puesta en producción del piloto
**Como** Fundador **quiero** el sistema en producción con 5-10 comercios reales
**para** validar el MVP con dinero real y métricas duras.

**Criterios de aceptación:**
- Despliegue reproducible documentado (guía paso a paso en el repo).
- Backup automático diario de la BD + restauración **ensayada** (no solo configurada).
- KYB/producción del proveedor aprobado; gatillos legales del plan maestro cumplidos.
- Métricas del criterio de éxito midiéndose desde la primera transacción.

---

## Resumen: mapa historia → sprint

| Sprint | Fase(s) roadmap | Historias |
|--------|-----------------|-----------|
| 0 | Fase 0 | T-001, T-002, T-003, T-004 |
| 1 | Fases 1-2 | HU-001, HU-002, HU-003, T-005 |
| 2 | Fase 3 | HU-004, HU-005, HU-006, HU-007 |
| 3 | Fase 4 | HU-008, T-006, HU-009 |
| 4 | Fase 5 | HU-010, HU-011, HU-012, HU-013 |
| 5 | Fases 6-7 | HU-014, HU-015, HU-016, HU-017 |
| 6 | Fase 8 | HU-018, HU-019, HU-020 |
| 7 | Fase 9 | HU-021, HU-022 |
| 8 | Fase 10 | HU-023, HU-024 |

> **Cómo usar este backlog:** al iniciar una historia, cambiar ⬜ → 🔵 y crear la rama
> `feature/HU-xxx-...`; al mergearla a `develop` con la DoD cumplida, 🔵 → ✅
> (commit `docs(gestion): completar HU-xxx`). Las historias se pueden dividir si
> resultan grandes — lo que no se negocia son sus criterios de aceptación.