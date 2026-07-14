# 02 — Arquitectura

## Enfoque

El proyecto usa **Arquitectura Hexagonal (Ports & Adapters)** con principios de
**DDD (Domain-Driven Design)** y **Clean Architecture**. Es un **monolito modular**
organizado **por contexto de negocio** y, dentro de cada contexto, **por capas**.

La meta central: **aislar la lógica de negocio** de frameworks, base de datos y
servicios externos, de modo que el dominio sea puro, testeable sin infraestructura,
y que cambiar un proveedor de pago o la base de datos no afecte al negocio.

---

## Las tres capas

```
        ┌─────────────────────────────────────────────────┐
        │                INFRAESTRUCTURA                   │
        │  (adaptadores: REST, JPA, proveedores externos)  │
        │   ┌───────────────────────────────────────────┐ │
        │   │              APLICACIÓN                    │ │
        │   │      (casos de uso / servicios)            │ │
        │   │   ┌─────────────────────────────────────┐ │ │
        │   │   │            DOMINIO                   │ │ │
        │   │   │  (entidades, VOs, reglas, puertos)  │ │ │
        │   │   │        SIN dependencias             │ │ │
        │   │   └─────────────────────────────────────┘ │ │
        │   └───────────────────────────────────────────┘ │
        └─────────────────────────────────────────────────┘

        Las flechas de dependencia SIEMPRE apuntan hacia adentro.
```

### 1. Dominio (el centro, sin dependencias externas)

Contiene la lógica de negocio pura. **No conoce** Spring, JPA, HTTP, ni ningún
framework. Si el dominio importa algo de `org.springframework` o `jakarta.persistence`,
está mal.

- **Entidades:** objetos con identidad y ciclo de vida (`OrdenDePago`, `Comercio`).
- **Value Objects (VO):** objetos inmutables sin identidad, definidos por su valor
  (`Dinero`, `ReferenciaPago`, `Comision`). Encapsulan validación.
- **Reglas de negocio:** las transiciones de estado, los cálculos, las invariantes.
- **Puertos (interfaces):** contratos de lo que el dominio necesita del exterior.
  - *Puertos de entrada* (driving): qué operaciones ofrece el núcleo. Ej:
    `CrearOrdenUseCase`, `ProcesarWebhookUseCase`.
  - *Puertos de salida* (driven): qué necesita el núcleo del mundo exterior. Ej:
    `OrdenDePagoRepositorio`, `ProveedorDePagoPort`, `NotificadorDeComercios`.

### 2. Aplicación (casos de uso)

Orquesta el dominio para cumplir un caso de uso concreto. Depende **solo del dominio**.
Cada caso de uso es una unidad pequeña con **una sola responsabilidad**.

- Implementa los *puertos de entrada* del dominio.
- Usa los *puertos de salida* (recibe sus implementaciones por inyección).
- **No contiene reglas de negocio** (esas viven en el dominio); coordina el flujo:
  cargar → aplicar regla del dominio → persistir → notificar.
- Maneja transacciones (`@Transactional` va aquí, no en el dominio).

### 3. Infraestructura (adaptadores)

Implementa los puertos con tecnología concreta. Es la única capa que conoce Spring,
JPA, HTTP, Binance, etc.

- **Adaptadores de entrada (driving):** `RestController` que reciben peticiones y
  webhooks, y llaman a los puertos de entrada.
- **Adaptadores de persistencia (driven):** repositorios JPA que implementan los
  puertos de repositorio del dominio. Aquí viven las **entidades JPA** (distintas de
  las entidades de dominio) y los *mappers* entre ambas.
- **Adaptadores de proveedor (driven):** implementaciones de `ProveedorDePagoPort`
  (ej. `BinancePayAdapter`).

---

## Regla de oro de dependencias

> **Las dependencias apuntan siempre hacia el dominio. El dominio no depende de nadie.**

- `dominio` → no depende de nada.
- `aplicacion` → depende de `dominio`. **Nunca** de `infraestructura`.
- `infraestructura` → depende de `aplicacion` y `dominio` (implementa sus puertos).

La **inversión de dependencias** (la "D" de SOLID) es lo que permite esto: el dominio
define la interfaz (`OrdenDePagoRepositorio`), la infraestructura la implementa
(`OrdenDePagoRepositorioJpa`), y Spring inyecta la implementación en tiempo de ejecución.

Estas reglas se **verifican automáticamente** con ArchUnit (ver Estrategia de Pruebas).

---

## Estructura de paquetes

Organización **por contexto primero, por capa después**. Cada contexto de negocio es
un paquete de alto nivel; dentro, las tres capas.

```
com.pasarela
├── PagosApplication.java              # arranque Spring (único punto con @SpringBootApplication)
│
├── compartido/                        # kernel compartido entre contextos
│   ├── dominio/                       #   VOs, tipos comunes (Dinero, IdComercio...) y
│   │                                  #   puertos que cruzan contextos (AutorizadorDeCobros,
│   │                                  #   CuentasDeAccesoPort, LiquidadorDeOrdenes, Bitácora)
│   └── infraestructura/               #   config transversal, manejo de errores, seguridad HTTP
│
├── pagos/                             # CONTEXTO: órdenes de pago, webhook, ventas
│   ├── dominio/
│   │   ├── modelo/                    #   OrdenDePago, EstadoOrden, ReferenciaPago...
│   │   └── puerto/
│   │       ├── entrada/               #   CrearOrdenUseCase, ProcesarWebhookUseCase...
│   │       └── salida/                #   OrdenDePagoRepositorio, ProveedorDePagoPort...
│   ├── aplicacion/                    #   CrearOrdenService, ProcesarWebhookService...
│   └── infraestructura/
│       ├── entrada/rest/              #   OrdenController, WebhookController, VentasController
│       ├── entrada/programacion/      #   PlanificadorDeExpiracion, PlanificadorDeReconciliacion
│       ├── salida/persistencia/       #   OrdenJpaEntity, OrdenDePagoRepositorioJpa, mapper
│       └── salida/proveedor/          #   ProveedorDePagoSimulado (BinancePayAdapter en Sprint 7)
│
├── comercios/                         # CONTEXTO: comercios y onboarding
│   ├── dominio/
│   ├── aplicacion/
│   └── infraestructura/
│
├── seguridad/                         # CONTEXTO: cuentas de acceso, JWT y roles
│   ├── dominio/                       #   Usuario, RolUsuario
│   ├── aplicacion/                    #   AutenticarUsuarioService, CrearCuentaComercioService
│   └── infraestructura/               #   AuthController, ServicioDeTokens (JWT), AdaptadorBCrypt
│
└── liquidaciones/                     # CONTEXTO: liquidación y conciliación
    ├── dominio/
    ├── aplicacion/
    └── infraestructura/
```

> **Nota de estilo Ceiba:** los nombres de paquete y clase del dominio van en
> **español**, reflejando el *lenguaje ubicuo* del negocio. El código técnico
> (frameworks, librerías) queda en la infraestructura.

---

## Flujo de una petición (ejemplo: crear orden)

```
1. HTTP POST /ordenes
        │
        ▼
2. OrdenController (infra/entrada)          ← adaptador de entrada
        │  convierte DTO → comando
        ▼
3. CrearOrdenUseCase (dominio/puerto)       ← interfaz (puerto de entrada)
        │  implementado por
        ▼
4. CrearOrdenService (aplicacion)           ← orquesta el caso de uso
        │  crea OrdenDePago (dominio) y usa puertos de salida
        ├─────────────► OrdenDePagoRepositorio (puerto salida) ──► OrdenRepositorioJpa (infra)
        └─────────────► ProveedorDePagoPort   (puerto salida) ──► BinancePayAdapter    (infra)
        │
        ▼
5. Devuelve resultado → Controller → DTO de respuesta → HTTP 201
```

El dominio y la aplicación **nunca** saben que hay HTTP, JPA o Binance detrás. Se puede
probar todo el caso de uso con implementaciones falsas de los puertos, sin levantar
Spring ni base de datos.

---

## Mapeo entre fronteras (mappers)

Cada frontera tiene su propia representación de los datos; **no se cruzan objetos**:

- **DTO** (REST) ↔ **Comando/Dominio**: en el controlador.
- **Entidad de dominio** ↔ **Entidad JPA**: en el adaptador de persistencia (mapper).

Esto evita que anotaciones de JPA o de serialización contaminen el dominio. Es más
código, pero es lo que mantiene el núcleo limpio y sustituible.

---

## Decisiones de arquitectura clave

| Decisión | Motivo | ADR |
|----------|--------|-----|
| Sin custodia de fondos | Legal, tributario (RST), riesgo | [ADR-001](adr/ADR-001-no-custodia.md) |
| Monolito modular, no microservicios | Simplicidad en MVP; modularidad da opción futura | [ADR-002](adr/ADR-002-monolito-modular.md) |
| Proveedores como adaptadores de un puerto | Cambiar/añadir proveedor sin tocar dominio | [ADR-003](adr/ADR-003-puerto-proveedor-pago.md) |
| Idempotencia estricta en webhooks | Evitar doble cobro/liquidación | [ADR-004](adr/ADR-004-idempotencia-webhooks.md) |

---

## Qué NO hacer (anti-patrones a evitar)

- ❌ Anotar entidades de dominio con `@Entity` o `@Table` (mezcla dominio e infra).
- ❌ Llamar a un repositorio JPA directamente desde un controlador (saltarse la aplicación).
- ❌ Poner reglas de negocio en el controlador o en el servicio de aplicación.
- ❌ Retornar entidades JPA en la respuesta REST (exponer el modelo de persistencia).
- ❌ Inyectar `HttpServletRequest` o tipos de Spring en el dominio.
- ❌ Un "God Service" con todos los casos de uso; uno por caso de uso.
