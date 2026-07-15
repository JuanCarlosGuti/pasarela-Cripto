# Gestión 03 — Plan de Sprints

> Sprints **por alcance, no por fechas** (principio del roadmap: "métricas cumplidas,
> no fechas"). Un sprint se cierra cuando su **objetivo verificable** se cumple, se
> mergea `develop` → `main` y se tagea. Las historias de cada sprint están detalladas
> en `02-backlog-historias-de-usuario.md`.

---

## Reglas del juego

1. **Un sprint a la vez.** No se abre el siguiente sin cerrar (o re-planificar
   explícitamente) el actual.
2. **Cerrar = mergear a `main` + tag.** El tag del sprint es el punto de
   restauración (`v0.<sprint>.0`).
3. **Cada sprint deja el sistema usable** y `./mvnw verify` en verde (principio
   "vertical antes que horizontal").
4. **El alcance se puede recortar, la calidad no.** Si un sprint crece, se mueven
   historias al siguiente; jamás se mergea sin sus pruebas.
5. **Mini-ceremonias de dev único** (15 min, por escrito en `NOTAS-SPRINT.md` o en
   la descripción del tag):
   - *Planning:* releer las HU del sprint y sus criterios antes de la primera rama.
   - *Review:* demo contra los criterios de aceptación (literalmente recorrerlos).
   - *Retro:* 3 líneas — qué funcionó, qué estorbó, qué cambio para el siguiente.

---

## Sprint 0 — Fundaciones ⚙️
**Objetivo:** esqueleto compilando con fronteras vigiladas y CI activo.
**Historias:** T-001, T-002, T-003, T-004 · **Tag de cierre:** `v0.0.1`

**Criterio de cierre (demo):**
- `docker compose up -d` + `./mvnw verify` + app arriba + `/actuator/health` UP.
- ArchUnit falla si se viola una frontera (demostrado con un cambio de sabotaje que
  luego se revierte).
- CI en verde en GitHub; ramas protegidas.

**Entregable de pruebas:** ArchUnit activo + el pipeline mismo.

---

## Sprint 1 — El corazón: dominio y persistencia de la Orden 💛
**Objetivo:** la máquina de estados de `OrdenDePago` completa, probada al 100% y
persistida en PostgreSQL real.
**Historias:** HU-001, HU-002, HU-003, T-005 · **Tag:** `v0.1.0`

**Criterio de cierre:**
- Toda la matriz de transiciones (válidas E inválidas) tiene prueba unitaria.
- Cobertura del dominio de pagos ≈ 100% líneas y ramas (aquí sí se exige).
- Ida y vuelta dominio↔JPA sin pérdidas contra Testcontainers.

**Entregable de pruebas:** suite unitaria del dominio (rápida, sin Spring) +
integración JPA. **Aquí se configura PIT (mutation testing)** sobre el dominio: si
matar un mutante no rompe ningún test, faltan tests.

---

## Sprint 2 — Comercios, onboarding y seguridad 🔐
**Objetivo:** un comercio se registra, un admin lo verifica, todo con JWT y roles.
**Historias:** HU-004, HU-005, HU-006, HU-007 · **Tag:** `v0.2.0`

**Criterio de cierre:**
- Flujo completo por HTTP: registro → login admin → verificación → login comercio.
- Aislamiento entre comercios y matriz de permisos probados endpoint por endpoint.
- VO `Nit` validando dígito de verificación con casos reales.

**Entregable de pruebas:** primera suite de API (MockMvc/RestAssured) + tests de
seguridad (401/403/expiración/manipulación de token).

---

## Sprint 3 — Cobro con QR (proveedor simulado) 🧾
**Objetivo:** un comercio verificado crea un cobro y obtiene QR, contra el
simulador.
**Historias:** HU-008, T-006, HU-009 · **Tag:** `v0.3.0`

**Criterio de cierre:**
- Demo end-to-end: login → crear orden → recibir QR → consultar estado.
- El simulador cubre éxito Y fallos (timeout, 500) y los tests de resiliencia del
  caso de uso pasan.
- Contrato de la API de órdenes documentado (springdoc-openapi) para el frontend
  Angular.

**Entregable de pruebas:** tests del caso de uso con fakes + API + resiliencia ante
proveedor caído.

---

## Sprint 4 — Webhook e idempotencia (EL sprint crítico) 🚨
**Objetivo:** la confirmación de pago es robusta, idempotente y a prueba de
concurrencia. **Este sprint no se recorta: se termina.**
**Historias:** HU-010, HU-011, HU-012, HU-013 · **Tag:** `v0.4.0`

**Criterio de cierre:**
- Los 6 caminos tristes de `docs/07` tienen test de API end-to-end.
- Test de **concurrencia real** (mismo evento en paralelo contra PostgreSQL) en verde.
- Constraint de unicidad verificada a nivel SQL.
- Demo: pagar una orden con el simulador y ver "PAGADO ✓".

**Entregable de pruebas:** la suite más exigente del proyecto — firma, duplicados,
concurrencia, fuera de orden, tardíos. PIT sobre `ProcesarWebhookPagoService`.

---

## Sprint 5 — Convergencia y dinero trazado 🔄💰
**Objetivo:** el sistema converge solo (jobs) y cada peso queda liquidado y
conciliado.
**Historias:** HU-014, HU-015, HU-016, HU-017 · **Tag:** `v0.5.0`

**Criterio de cierre:**
- Simulación de webhook perdido: la reconciliación confirma la orden sola.
- Carrera expiración-vs-pago resuelta limpiamente (test).
- Liquidación cuadra al centavo; discrepancias se detectan, no se tapan.

**Entregable de pruebas:** tests de jobs (idempotencia, lotes, carreras) +
propiedades aritméticas de la liquidación.

---

## Sprint 6 — Dashboard del comercio 📊
**Objetivo:** el comercio ve sus ventas, exporta el historial y tiene comprobantes.
**Historias:** HU-018, HU-019, HU-020 · **Tag:** `v0.6.0`

**Criterio de cierre:**
- Demo con datos realistas: ventas día/mes, export CSV abierto en Excel real,
  comprobante generado.
- Aislamiento por comercio re-verificado en cada endpoint nuevo.

**Entregable de pruebas:** API + validación del CSV (encoding/separador) + reglas de
agregación.

---

## Sprint 7 — Binance Pay real (sandbox) 🟡
**Objetivo:** pago real de extremo a extremo en el sandbox de Binance; el simulador
se apaga con una property.
**Historias:** HU-021, HU-022 · **Tag:** `v0.7.0`
**Dependencia externa:** acceso al merchant sandbox de Binance (tramitarlo DESDE el
Sprint 5 para no bloquear).

**Criterio de cierre:**
- Pago real en sandbox: QR → pago con app Binance → webhook real → "PAGADO ✓".
- El diff del sprint no toca `dominio/` ni `aplicacion/` (valida ADR-003).
- Endurecimiento: rate limiting, gitleaks limpio, dependencias sin críticas.

**Entregable de pruebas:** tests de contrato WireMock con payloads reales grabados +
suite manual sandbox documentada.

---

## Sprint 8 — Piloto en producción 🚀
**Objetivo:** MVP operando con comercios reales y métricas midiéndose.
**Historias:** HU-023, HU-024 · **Tag:** **`v1.0.0`**
**Dependencias externas:** KYB producción aprobado + gatillos legales del plan
maestro (abogado fintech, contratos).

**Criterio de cierre:**
- Primera transacción real de un comercio del piloto, conciliada.
- Alertas funcionando (probadas provocándolas), backup restaurado en ensayo.
- Métricas del criterio de éxito de `docs/01` reportándose.

---

## Gestión de riesgos del plan

| Riesgo | Señal | Acción |
|--------|-------|--------|
| Sprint que no cierra (se estanca) | > 2-3 semanas sin avanzar historias | Re-planning: dividir historias, recortar alcance del sprint (no la calidad) |
| Bloqueo por sandbox Binance | Sin acceso al llegar al Sprint 7 | Tramitar acceso desde Sprint 5; el simulador permite avanzar Sprint 8 parcial (observabilidad) |
| Descubrimientos que cambian el diseño | Un criterio de aceptación resulta imposible/incorrecto | Registrar ADR nuevo, ajustar backlog con commit `docs(gestion)` — decisión trazada, no silenciosa |
| Scope creep (ideas nuevas) | Ganas de agregar features fuera del MVP | Anotarlas en `POST-MVP.md` del backlog y seguir; el alcance del MVP lo define `docs/01` |

---

## Estado actual

| Sprint | Estado | Tag |
|--------|--------|-----|
| 0 — Fundaciones | ✅ cerrado | `v0.0.1` |
| 1 — Dominio + persistencia | ✅ cerrado | `v0.1.0` |
| 2 — Comercios + seguridad | ✅ cerrado | `v0.2.0` |
| 3 — Cobro QR simulado | ✅ cerrado | `v0.3.0` |
| 4 — Webhook + idempotencia | ✅ cerrado | `v0.4.0` |
| 5 — Jobs + liquidación | ✅ cerrado | `v0.5.0` |
| 6 — Dashboard | ✅ cerrado | `v0.6.0` |
| 7 — Binance real | 🔵 en curso | — |
| 8 — Piloto | ⬜ pendiente | — |

> Actualizar esta tabla al abrir/cerrar cada sprint (commit `docs(gestion)`).