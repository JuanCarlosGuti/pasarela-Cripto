# ADR-002 — Monolito modular en lugar de microservicios

**Estado:** Aceptado

## Contexto

El equipo tiene experiencia y afinidad con microservicios. Existe la tentación de
arrancar el proyecto con esa arquitectura. Sin embargo, el proyecto está en fase de
**MVP**, con un solo desarrollador principal y la necesidad de llegar rápido a un piloto
con comercios reales.

Los microservicios traen costos operativos altos (despliegue distribuido, comunicación
entre servicios, observabilidad, consistencia) que no se justifican antes de tener
tracción ni escala.

## Decisión

El MVP se construye como un **monolito modular**: un solo despliegue, pero
internamente organizado por **contextos de negocio** (`pagos`, `comercios`,
`liquidaciones`) con **arquitectura hexagonal** dentro de cada uno.

La modularidad interna y las fronteras estrictas (verificadas con ArchUnit) mantienen
el código desacoplado, de modo que **si en el futuro se justifica**, extraer un
contexto como microservicio sea un refactor acotado, no una reescritura.

## Consecuencias

**A favor:**
- Simplicidad operativa: un despliegue, una base de datos, una configuración.
- Desarrollo y depuración más rápidos.
- Camino más corto al piloto.
- La modularidad deja abierta la puerta a microservicios futuros sin pagar su costo hoy.

**En contra:**
- Escala como una sola unidad (aceptable en MVP).
- Requiere disciplina para no romper las fronteras entre contextos (mitigado con
  ArchUnit).

## Revisión futura

Reconsiderar solo cuando exista tracción real y un cuello de botella concreto que un
servicio separado resuelva. No antes.
