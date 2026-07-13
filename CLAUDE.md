# CLAUDE.md — Pasarela de Pagos Cripto

> Este archivo lo lee Claude Code al inicio de cada sesión. Define el contexto,
> las reglas y las convenciones del proyecto. Mantenerlo **conciso**: cada línea
> se lee en cada sesión y consume contexto.

---

## 1. Qué es este proyecto

Pasarela de pagos que permite a comercios en Colombia **cobrar en criptomonedas
y recibir pesos colombianos (COP)** en su cuenta, sin tocar cripto ni asumir
volatilidad. "Tan fácil como cobrar con Nequi, pero para cripto".

- **Cliente que paga la plataforma:** el comercio (comisión ~2-2.5% por transacción).
- **Usuario que paga en la tienda:** el pagador, con su saldo cripto (Binance u otra wallet).
- **Ingreso del negocio:** margen ~1-1.5% tras el costo del proveedor de conversión.
- **Rol de la plataforma:** *orquestar* el pago. La conversión cripto→COP, la
  custodia y el KYC los ejecuta un **proveedor de rampa de terceros ya regulado**,
  que liquida COP **directamente al comercio**.

Contexto competitivo (resumen): el riel con más usuarios en Colombia es Binance Pay
(500+ comercios ya lo usan); el competidor más parecido es Payválida (enfoque
e-commerce); nuestro diferencial es la última milla del comercio físico pequeño
(onboarding en minutos, soporte local, reportes para el contador). Detalle en
`docs/negocio/03-estado-del-arte.md`.

---

## 2. REGLA DE ORO (nunca violar)

**La plataforma NUNCA custodia fondos ni criptoactivos de terceros.**

- No recibimos, guardamos, convertimos ni administramos cripto ni COP de nadie.
- El proveedor de rampa recibe la cripto, convierte y **liquida COP directamente
  al comercio**. Nosotros solo registramos y conciliamos.
- Ningún flujo de dinero de terceros puede pasar por cuentas de la plataforma.
  (Requisito legal, tributario —Régimen Simple— y de diseño. No negociable.)

Si una tarea implicaría custodiar fondos, **detente y avísame** antes de continuar.

---

## 3. Stack técnico

- **Backend:** Java 25+ · Spring Boot 3 · Maven
- **Persistencia:** PostgreSQL · Spring Data JPA · Flyway (migraciones)
- **Frontend:** Angular (repositorio separado; este repo es el backend)
- **Infra local:** Docker Compose (PostgreSQL)
- **Testing:** JUnit 5 · Mockito · Testcontainers · ArchUnit
- **Calidad:** SonarQube/SonarCloud

Este repositorio es un **monolito modular**, NO microservicios (decisión registrada
en `docs/adr/ADR-002-monolito-modular.md`).

---

## 4. Arquitectura — Hexagonal (Ports & Adapters) + DDD

Separación estricta entre dominio e infraestructura. Paquetes por contexto de
negocio (`compartido`, `pagos`, `comercios`, `liquidaciones`) y, dentro de cada uno:

```
com.pasarela.<contexto>
├── dominio/
│   ├── modelo/        # Entidades y VOs. SIN Spring, SIN JPA, SIN frameworks.
│   └── puerto/
│       ├── entrada/   # Casos de uso (interfaces): CrearOrdenUseCase...
│       └── salida/    # Lo que el dominio necesita: repositorios, ProveedorDePagoPort...
├── aplicacion/        # Servicios que implementan los casos de uso. @Transactional aquí.
└── infraestructura/
    ├── entrada/rest/          # Controllers, webhooks
    ├── salida/persistencia/   # Entidades JPA (separadas del dominio) + mappers
    └── salida/proveedor/      # Adaptadores de proveedores (BinancePayAdapter...)
```

**Reglas de dependencia (verificadas con ArchUnit):**
- `dominio` no depende de NADA externo (ni Spring, ni JPA, ni Lombok en el modelo puro).
- `aplicacion` depende del `dominio`, nunca de `infraestructura`.
- `infraestructura` implementa los puertos (inversión de dependencias).
- Los proveedores de pago son **adaptadores de un mismo puerto** (`ProveedorDePagoPort`):
  añadir/cambiar proveedor = nuevo adaptador, cero cambios en el dominio.

Detalle completo en `docs/02-arquitectura.md`.

---

## 5. Concepto central: la Orden de Pago

Entidad núcleo. Su ciclo de vida es una **máquina de estados**:

```
CREADA → PENDIENTE_PAGO → PAGO_DETECTADO → CONVERTIDA → LIQUIDADA
                  │
                  ├── (expira sin pago) → EXPIRADA
                  └── (error/pago inválido) → FALLIDA → EN_REVISION
```

- El estado solo cambia mediante métodos de dominio que validan la transición
  (`confirmarPago`, `expirar`, `marcarComoLiquidada`...). Nunca `setEstado` público.
- Guardar timestamps de cada transición (auditoría y conciliación).
- Modelo completo (entidades, VOs, contextos) en `docs/04-modelo-de-dominio.md`.

---

## 6. Reglas críticas de implementación

### Idempotencia en webhooks (LO MÁS DELICADO)
1. **Validar la firma** del webhook antes de procesarlo.
2. Guardar el **evento crudo** (`EventoProveedor`) ANTES de procesarlo.
3. **Idempotencia estricta:** clave única `(proveedor, idExternoEvento)` con
   restricción de unicidad en BD. Procesar dos veces el mismo evento NO puede
   duplicar nada (ni doble confirmación ni doble liquidación).
4. **Job de reconciliación** de respaldo: el sistema converge al estado correcto
   aunque un webhook nunca llegue.
5. Caminos tristes obligatorios: firma inválida, evento duplicado, orden inexistente,
   monto errado, pago tardío tras expiración, webhooks fuera de orden.

### Seguridad y datos
- Nunca datos personales/sensibles en URLs, query params ni logs.
- Nunca secretos hardcodeados: variables de entorno siempre.
- Spring Security + JWT; roles: `ADMIN`, `COMERCIO`.

### Cumplimiento (desde el MVP)
- Límites configurables por transacción y por comercio/mes.
- Bitácora de operaciones inusuales.
- El KYC del pagador lo hace el proveedor; nosotros verificamos datos básicos del comercio.

---

## 7. Convenciones de código (estilo Ceiba / clean code)

- **Dominio en español** (lenguaje ubicuo): `OrdenDePago`, `Comercio`,
  `calcularComision()`, `estaExpirada()`.
- **TDD:** primero la prueba, luego la implementación. Sin prueba no hay código.
- Value Objects sobre primitivos (`Dinero`, no `BigDecimal` suelto).
- Sin setters públicos en entidades de dominio; el estado cambia por métodos de negocio.
- Excepciones de dominio con significado (`OrdenNoPuedeConfirmarseException`).
- Métodos cortos, una responsabilidad; un caso de uso por clase de aplicación.
- Commits pequeños y descriptivos.
- Guía completa en `docs/03-guia-estilo-clean-code.md` · Pruebas en
  `docs/06-estrategia-de-pruebas.md`.

---

## 8. Cómo trabajar conmigo (Juan Carlos)

- Explícame el **por qué** de las decisiones, no solo el cómo. Soy arquitecto de
  software y quiero entender, no solo recibir código.
- Antes de cambios grandes o multi-archivo, **muéstrame el plan** primero.
- Muéstrame siempre el diff antes de aplicar; reviso antes de aceptar.
- Construir por **incrementos pequeños y verificables**, no todo de golpe.
- Si algo roza la REGLA DE ORO (sección 2) o el cumplimiento legal, párate y avísame.
- Responde en español.

---

## 9. Documentación del proyecto (fuente de verdad)

| Documento | Ruta |
|---|---|
| Visión y alcance del MVP | `docs/01-vision-y-alcance-mvp.md` |
| Arquitectura | `docs/02-arquitectura.md` |
| Guía de estilo / clean code | `docs/03-guia-estilo-clean-code.md` |
| Modelo de dominio | `docs/04-modelo-de-dominio.md` |
| Servicios y casos de uso | `docs/05-servicios-y-casos-de-uso.md` |
| Estrategia de pruebas | `docs/06-estrategia-de-pruebas.md` |
| Integración de proveedores | `docs/07-integracion-proveedores.md` |
| Roadmap de desarrollo (fases 0-10) | `docs/08-roadmap-desarrollo.md` |
| Decisiones de arquitectura | `docs/adr/` |
| Documentos de negocio | `docs/negocio/` |
| **Flujo de trabajo Git (GitFlow simplificado)** | `docs/gestion/01-flujo-de-trabajo-git.md` |
| **Backlog de historias de usuario (fuente de verdad)** | `docs/gestion/02-backlog-historias-de-usuario.md` |
| **Plan de sprints (por alcance, no fechas)** | `docs/gestion/03-plan-de-sprints.md` |
| **Herramientas del proyecto** | `docs/gestion/04-herramientas.md` |

Reglas de trabajo derivadas:
- Cada historia se desarrolla en una rama `feature/HU-xxx-...` desde `develop`
  (nunca commitear directo a `main` ni `develop`). Commits: Conventional Commits
  en español (ver `docs/gestion/01`).
- Al terminar una HU: actualizar su estado en el backlog (`docs/gestion/02`) con un
  commit `docs(gestion): ...`.
- Al cerrar un sprint: merge `develop`→`main` + tag `v0.<sprint>.0` y actualizar la
  tabla de estado en `docs/gestion/03`.

Ante cualquier duda de diseño, **consulta primero estos documentos** antes de asumir.

---

## 10. Estado actual del proyecto

<!-- Actualizar esta sección a medida que avanza el proyecto -->

- [x] Proyecto Spring Boot generado (Maven, paquete base `com.pasarela`)
- [x] Documentación técnica y de negocio completa en `docs/`
- [x] **Fase 0:** pom.xml completo, estructura hexagonal de paquetes, compose.yaml
      (PostgreSQL), Flyway, ArchUnit, healthcheck, CI (Sprint 0 cerrado: `v0.0.1`)
- [x] **Fase 1:** dominio de `OrdenDePago` (VOs, máquina de estados, tests) — `v0.1.0`
- [x] **Fase 2:** persistencia de la orden (puerto + JPA + Testcontainers) — `v0.1.0`
- [x] **Fase 3:** comercios y onboarding + seguridad JWT — `v0.2.0`
- [x] **Fase 4:** crear orden + QR (con proveedor simulado) — `v0.3.0`
- [x] **Fase 5:** webhook + idempotencia + notificación — `v0.4.0`
- [x] **Fase 6:** jobs de expiración y reconciliación — `v0.5.0`
- [x] **Fase 7:** liquidación y conciliación + comisión — `v0.5.0`
- [ ] **Fase 8:** dashboard del comercio — 🔵 en curso (Sprint 6): ventas del
      día/mes ✅ (HU-018), exportar CSV 🔵 (HU-019), comprobantes ⬜ (HU-020)
- [ ] **Fase 9:** integración real Binance Pay (sandbox → producción)
- [ ] **Fase 10:** piloto con comercios reales

Backlog y estado de sprints al detalle: `docs/gestion/02` y `docs/gestion/03`.

**MVP con un solo riel: Binance Pay.** El riel on-chain (USDT/USDC vía rampa) y la
liquidación vía Bre-B vienen después, como nuevos adaptadores del mismo puerto.
