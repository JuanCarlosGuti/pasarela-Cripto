# Pasarela de Pagos Cripto

> Pasarela que permite a comercios en Colombia **cobrar en criptomonedas y recibir
> pesos colombianos (COP)** en su cuenta, sin tocar cripto ni asumir volatilidad.

[![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow)]()
[![Java](https://img.shields.io/badge/Java-25+-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)]()
[![Arquitectura](https://img.shields.io/badge/arquitectura-hexagonal-blue)]()

---

## ¿Qué resuelve?

Millones de colombianos tienen saldo en criptomonedas (Binance, wallets) pero casi
ningún comercio lo acepta, porque los negocios no quieren volatilidad, contabilidad
cripto ni riesgo legal. Esta plataforma cierra esa brecha: el comercio cobra
escaneando un QR y **recibe pesos en su cuenta**, mientras el pagador usa su cripto
como si fuera dinero normal.

- **Cliente de la plataforma:** el comercio (paga comisión por transacción).
- **Usuario final:** el pagador, que usa su saldo cripto.
- **Ingreso:** comisión porcentual sobre cada transacción liquidada.

## Principio fundamental

> **La plataforma NUNCA custodia fondos ni criptoactivos.**
> Solo *orquesta* el pago. La conversión cripto→COP, la custodia y el KYC los ejecuta
> un proveedor de rampa de terceros ya regulado.

Esto no es solo una decisión técnica: es un requisito legal, tributario y de diseño.
Ver [ADR-001](docs/adr/ADR-001-no-custodia.md).

---

## Documentación

| Documento | Descripción |
|-----------|-------------|
| [Visión y Alcance del MVP](docs/01-vision-y-alcance-mvp.md) | Qué construimos, qué NO, criterios de éxito |
| [Arquitectura](docs/02-arquitectura.md) | Hexagonal + DDD, capas, reglas de dependencia |
| [Guía de Estilo y Clean Code](docs/03-guia-estilo-clean-code.md) | Convenciones al estilo Ceiba |
| [Modelo de Dominio](docs/04-modelo-de-dominio.md) | Entidades, value objects, máquina de estados |
| [Servicios y Casos de Uso](docs/05-servicios-y-casos-de-uso.md) | Casos de uso del MVP, puertos |
| [Estrategia de Pruebas](docs/06-estrategia-de-pruebas.md) | TDD, pirámide de tests, Testcontainers |
| [Integración de Proveedores](docs/07-integracion-proveedores.md) | Binance Pay, rampa, webhooks, idempotencia |
| [Roadmap de Desarrollo](docs/08-roadmap-desarrollo.md) | Fases e incrementos hasta el MVP |
| [Decisiones de Arquitectura (ADRs)](docs/adr/) | Registro de decisiones importantes |

### Gestión del proyecto

| Documento | Descripción |
|-----------|-------------|
| [Flujo de Trabajo Git](docs/gestion/01-flujo-de-trabajo-git.md) | GitFlow simplificado, Conventional Commits, recuperación |
| [Backlog de Historias de Usuario](docs/gestion/02-backlog-historias-de-usuario.md) | Épicas, HU con criterios de aceptación Gherkin |
| [Plan de Sprints](docs/gestion/03-plan-de-sprints.md) | Sprints 0-8 por alcance, criterios de cierre |
| [Herramientas](docs/gestion/04-herramientas.md) | Stack libre/gratuito por sprint |

---

## Stack técnico

- **Backend:** Java 25+ · Spring Boot 3 · Maven
- **Persistencia:** PostgreSQL · Spring Data JPA · Flyway
- **Frontend:** Angular (repositorio separado)
- **Infra local:** Docker Compose
- **Testing:** JUnit 5 · Mockito · Testcontainers · ArchUnit
- **Calidad:** SonarQube/SonarCloud

## Arranque rápido (local)

```bash
# 1. Levantar la base de datos
docker compose up -d

# 2. Compilar y correr las pruebas
./mvnw clean verify

# 3. Levantar la aplicación
./mvnw spring-boot:run
```

La aplicación queda en `http://localhost:8080`. El healthcheck de Actuator en
`http://localhost:8080/actuator/health`.

---

## Estructura del repositorio

```
pagos/
├── docs/                    # Documentación del proyecto (esta carpeta)
├── src/
│   ├── main/java/com/pasarela/
│   │   ├── <contexto>/      # Un paquete por contexto de negocio
│   │   │   ├── dominio/
│   │   │   ├── aplicacion/
│   │   │   └── infraestructura/
│   │   └── PagosApplication.java
│   └── test/java/com/pasarela/
├── CLAUDE.md                # Reglas para el asistente de código
├── compose.yaml             # Docker Compose (PostgreSQL)
└── pom.xml
```

## Contribuir

Antes de escribir código, lee la [Guía de Estilo y Clean Code](docs/03-guia-estilo-clean-code.md)
y la [Estrategia de Pruebas](docs/06-estrategia-de-pruebas.md). El proyecto sigue TDD:
primero la prueba, luego la implementación.
