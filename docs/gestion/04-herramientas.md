# Gestión 04 — Herramientas (software libre / gratis para emprender)

> Stack de herramientas con costo **$0** en la etapa MVP/piloto, priorizando open
> source y planes gratuitos generosos. Columna "Cuándo" indica en qué sprint se
> adopta — no instalar nada antes de necesitarlo.

---

## Código y colaboración

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **Git + GitHub Free** | Repos privados ilimitados, Actions (2.000 min/mes), Issues, Projects, Dependabot | Gratis | Ya |
| **GitHub Actions** | CI: `verify` en cada push (Linux runner soporta Testcontainers) | Incluido | Sprint 0 |
| **IntelliJ IDEA Community** | IDE Java completo, open source | Gratis | Ya |
| **Claude Code** | Par de programación con las reglas de `CLAUDE.md` | Ya lo usas | Ya |

## Build, calidad y análisis estático

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **Maven Wrapper (mvnw)** | Build reproducible sin instalar Maven | Libre | Sprint 0 |
| **JaCoCo** | Cobertura de código integrada al build | Libre | Sprint 0 |
| **ArchUnit** | Las fronteras hexagonales como test | Libre | Sprint 0 |
| **SonarCloud** | Análisis de calidad continuo. *Gratis solo para repos públicos*; para repo privado usar **SonarQube Community** en Docker local | Gratis | Sprint 1 |
| **Spotless + google-java-format** | Formateo automático en el build (cero discusiones de estilo) | Libre | Sprint 0 |
| **PIT (pitest)** | **Mutation testing**: mide si tus tests detectan bugs de verdad, no solo si ejecutan líneas. Clave para "tests muy reales" | Libre | Sprint 1 (dominio) |

## Pruebas

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **JUnit 5 + AssertJ + Mockito** | Base de la pirámide | Libre | Sprint 0 |
| **Testcontainers** | PostgreSQL real en tests (no H2 "que miente") | Libre | Sprint 1 |
| **WireMock** | Simular la API de Binance Pay en tests: respuestas grabadas, timeouts, errores. La pieza que hace "reales" los tests del adaptador | Libre | Sprint 3 (simulador) y 7 (contrato real) |
| **Instancio** o builders manuales | Datos de prueba (Object Mother) | Libre | Sprint 1 |
| **k6** (Grafana) | Pruebas de carga del webhook y la API antes del piloto | Libre (OSS) | Sprint 7 |

## API y base de datos

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **springdoc-openapi** | Documentación viva de la API (Swagger UI) — el contrato para el frontend Angular | Libre | Sprint 3 |
| **Bruno** | Cliente API open source (alternativa a Postman); las colecciones se versionan **en el repo** como archivos — coherente con "nada vive fuera de git". Colección: `coleccion-api/` | Libre | Adoptada (Sprint 1) |
| **DBeaver Community** | Cliente universal de BD | Libre | Sprint 0 |
| **Flyway Community** | Migraciones versionadas | Libre | Sprint 0 |

## Seguridad

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **Dependabot** | Alertas y PRs automáticos por dependencias vulnerables | Incluido en GitHub | Sprint 0 |
| **gitleaks** | Detectar secretos en el repo (hook local + paso de CI) | Libre | Sprint 0 |
| **OWASP Dependency-Check** | Escaneo profundo de CVEs en el build (complementa Dependabot) | Libre | Sprint 7 |

## Webhooks en desarrollo local

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **Cloudflare Tunnel** (`cloudflared`) | Exponer `localhost:8080` a internet para recibir webhooks del sandbox de Binance. Gratis y sin límites molestos (alternativa: ngrok free) | Gratis | Sprint 7 |

## Observabilidad (piloto)

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **Spring Actuator + Micrometer** | Métricas y health integrados | Libre | Sprint 0 (health) / 8 (métricas) |
| **Grafana + Loki + Prometheus** (docker compose) | Logs estructurados + métricas + alertas, autohospedado | Libre | Sprint 8 |
| **UptimeRobot** (plan free) | Ping externo al health: avisa si el sitio se cae | Gratis (50 monitores) | Sprint 8 |

## Infraestructura del piloto (elegir en Sprint 7-8)

Opciones de bajo costo para un monolito + PostgreSQL (evaluar al llegar):

- **VPS barato** (Hetzner ~€5/mes, o proveedores locales) + Docker Compose +
  Caddy/Traefik para TLS automático. Máximo control, mínimo costo. ★ Recomendada
  para el piloto.
- **Fly.io / Railway / Render**: PaaS con planes de entrada baratos, menos operación.
- **Oracle Cloud Free Tier**: VMs ARM gratuitas de por vida — gratis total, con la
  contrapartida de depender de disponibilidad de capacidad.
- **Base de datos**: PostgreSQL en el mismo VPS con backup diario a objeto storage
  (Backblaze B2 tiene 10 GB gratis) — o Neon/Supabase free tier si se prefiere
  gestionada.

> Principio: **el piloto no necesita Kubernetes.** Un VPS con Docker Compose,
> backups probados y alertas es más robusto que una plataforma compleja a medio
> configurar (coherente con ADR-002).

## Gestión del proyecto

| Herramienta | Para qué | Costo | Cuándo |
|---|---|---|---|
| **Markdown en `docs/gestion/` + git** | Backlog, sprints, decisiones — fuente de verdad versionada | Libre | Ya (decidido) |
| **GitHub Projects** | Tablero Kanban visual si algún día se quiere vista de tarjetas (sin migrar nada: enlaza a los md) | Gratis | Opcional |
| **draw.io / Excalidraw** | Diagramas (se exportan como .svg/.png al repo) | Libre | Cuando toque |

---

## Anti-lista: lo que NO usar todavía

- ❌ **Jira** — burocracia para un equipo de uno; el backlog en markdown cumple.
- ❌ **Kubernetes / microservicios de infra** — contradice ADR-002.
- ❌ **Kafka/RabbitMQ** — los jobs programados del MVP no lo necesitan.
- ❌ **Suites de pago** (Postman equipos, Sonar developer edition…) — cuando haya
  equipo e ingresos se reevalúa.