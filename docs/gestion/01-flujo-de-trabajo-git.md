# Gestión 01 — Flujo de Trabajo Git (GitFlow simplificado)

> Cómo se versiona este proyecto: ramas, commits, tags y recuperación ante
> desastres. Adaptación de GitFlow para un **desarrollador único** que quiere la
> estructura completa sin la burocracia que no aporta.

---

## Las ramas

```
main ────●───────────●──────────●────► SIEMPRE estable y desplegable
          \         / (v0.1.0)  / (v0.2.0)
develop ───●───●───●───●───●───●─────► integración: aquí se acumula el sprint
            \     /     \     /
feature/     ●───●       ●───●        una rama por historia de usuario
HU-001                HU-002
```

| Rama | Propósito | Regla |
|------|-----------|-------|
| `main` | Código **estable**. Cada merge = fin de sprint verificado. | Nunca se commitea directo. Solo merges desde `develop` (o `hotfix/`). Cada merge lleva **tag** `vX.Y.Z`. |
| `develop` | Integración continua del sprint en curso. | Siempre en verde (`./mvnw verify` pasa). Se mergean aquí las features terminadas. |
| `feature/HU-xxx-descripcion` | Una historia de usuario (o tarea técnica). | Nace de `develop`, muere al mergearse a `develop`. Vida corta (días, no semanas). |
| `hotfix/descripcion` | Corrección urgente sobre `main`. | Nace de `main`, se mergea a `main` **y** a `develop`. |

**Sin ramas `release/`:** al trabajar solo, la estabilización del sprint ocurre en
`develop`; cuando el criterio de fin de sprint se cumple, se mergea a `main` y se
tagea. Si algún día hay equipo y despliegues concurrentes, se añaden ramas release
(el modelo lo permite sin cambiar nada más).

## Nombres de rama

```
feature/HU-004-crear-orden-de-pago
feature/T-001-estructura-hexagonal      # T-xxx = tarea técnica sin historia de usuario
hotfix/webhook-firma-nula
```

Siempre en minúsculas, con el ID de la historia/tarea del backlog
(`docs/gestion/02-backlog-historias-de-usuario.md`).

---

## Commits: Conventional Commits (en español)

Formato: `<tipo>(<ámbito>): <descripción en imperativo>`

```
feat(pagos): crear entidad OrdenDePago con máquina de estados
fix(webhooks): rechazar evento con firma inválida antes de persistir
test(pagos): cubrir transiciones inválidas de EstadoOrden
refactor(comercios): extraer VO Nit con validación de dígito
docs(adr): añadir ADR-005 estrategia de ramas
chore(build): añadir dependencias de testcontainers al pom
ci(actions): ejecutar verify en cada push
```

| Tipo | Cuándo |
|------|--------|
| `feat` | Nueva funcionalidad de negocio |
| `fix` | Corrección de un bug |
| `test` | Solo pruebas (¡en TDD el commit de la prueba puede ir primero!) |
| `refactor` | Cambio interno sin alterar comportamiento |
| `docs` | Documentación |
| `chore` | Mantenimiento (deps, config) |
| `ci` | Pipeline |

**Ámbitos válidos:** `pagos`, `comercios`, `liquidaciones`, `compartido`, `webhooks`,
`seguridad`, `build`, `adr`, `gestion`.

Reglas:
- **Commits pequeños y frecuentes**: cada ciclo TDD (red→green→refactor) puede ser
  un commit. Un commit compila y sus tests pasan.
- La descripción dice **qué hace el commit**, no "cambios varios".
- Cuerpo opcional para el **por qué** cuando no es obvio.

## Versionado (SemVer pre-1.0)

- `v0.<sprint>.<hotfix>` durante el MVP: cierre del Sprint 1 → `v0.1.0`; un hotfix
  sobre él → `v0.1.1`.
- `v1.0.0` = MVP en producción con el piloto (Fase 10).

---

## Ciclo de trabajo diario

```bash
# 1. Empezar una historia
git switch develop && git pull
git switch -c feature/HU-004-crear-orden-de-pago

# 2. Ciclos TDD con commits pequeños
git add -A && git commit -m "test(pagos): orden pendiente pasa a PAGO_DETECTADO al confirmar"
git add -A && git commit -m "feat(pagos): implementar confirmarPago con validación de transición"

# 3. Respaldar el trabajo en curso (¡todos los días!)
git push -u origin feature/HU-004-crear-orden-de-pago

# 4. Terminar la historia: verificar TODO antes de mergear
./mvnw verify                       # unitarias + integración + ArchUnit en verde

# 5. Mergear a develop (merge explícito, conserva la historia de la rama)
git switch develop
git merge --no-ff feature/HU-004-crear-orden-de-pago
git push
git branch -d feature/HU-004-crear-orden-de-pago
git push origin --delete feature/HU-004-crear-orden-de-pago

# 6. Actualizar el backlog: marcar la HU como terminada (commit docs(gestion))
```

## Cierre de sprint

```bash
# Con el criterio de fin de sprint cumplido y develop en verde:
git switch main
git merge --no-ff develop -m "release: cierre Sprint 1 — núcleo de dominio OrdenDePago"
git tag -a v0.1.0 -m "Sprint 1: dominio OrdenDePago completo y probado"
git push origin main --tags
git switch develop
```

## Hotfix (bug en main)

```bash
git switch main
git switch -c hotfix/webhook-firma-nula
# ... fix + test que lo demuestra ...
git switch main && git merge --no-ff hotfix/webhook-firma-nula
git tag -a v0.1.1 -m "hotfix: NPE al validar firma nula en webhook"
git push origin main --tags
git switch develop && git merge --no-ff hotfix/webhook-firma-nula && git push
git branch -d hotfix/webhook-firma-nula
```

---

## Red de seguridad: que nada se pierda

### Capas de protección (de la más cotidiana a la más extrema)

1. **Push diario**: la rama en curso se sube a GitHub **todos los días** aunque esté
   a medias. Un disco dañado no puede costar más de un día de trabajo.
2. **`develop` siempre en verde**: si un experimento sale mal, `git switch develop`
   y se recomienza la feature. Las ramas feature son desechables.
3. **Tags por sprint**: cada `vX.Y.Z` es un punto de restauración probado. Volver a
   un estado bueno conocido: `git switch -c rescate v0.1.0`.
4. **GitHub como respaldo remoto**: todo lo importante (main, develop, features
   activas, tags) vive también fuera de esta máquina.

### Recetas de recuperación

| Desastre | Receta |
|----------|--------|
| "Borré cambios sin commitear" | Si estaban staged: `git fsck --lost-found`. Si no: no hay red — por eso commits pequeños y frecuentes. |
| "Hice commits en la rama equivocada" | `git switch rama-correcta && git cherry-pick <hashes>`, luego en la equivocada `git reset --hard origin/rama`. |
| "Un merge salió mal y ya lo commiteé" | `git reset --hard ORIG_HEAD` (inmediato) o `git revert -m 1 <hash-merge>` (si ya hiciste push). |
| "Perdí una rama / hice reset --hard por error" | `git reflog` → encontrar el hash previo → `git switch -c rescate <hash>`. El reflog guarda ~90 días de historia local. |
| "Necesito volver a como estaba en el sprint pasado" | `git switch -c rescate v0.1.0` y comparar con `git diff v0.1.0 develop`. |
| "Rompí el repo local por completo" | Clonar de nuevo desde GitHub: `git clone <url>`. Solo se pierde lo no pusheado (→ capa 1). |
| "Un commit ya pusheado a main tiene un bug" | **Nunca** `push --force` a main. Usar `git revert <hash>` o un `hotfix/`. |

### Reglas de oro

- ❌ `git push --force` a `main` o `develop`: **prohibido**. (En features propias,
  `--force-with-lease` es aceptable si nadie más las usa.)
- ❌ Rebase de ramas ya pusheadas a main/develop.
- ✅ Ante la duda, crear una rama: las ramas son gratis, el trabajo perdido no.
- ✅ Antes de operaciones destructivas (`reset --hard`, `clean`), un
  `git stash` o una rama de respaldo: `git branch backup-antes-de-X`.

---

## Protecciones en GitHub (configurar una vez)

En *Settings → Branches → Branch protection rules* para `main`:

- ✅ Require status checks to pass (el workflow de CI `verify`).
- ✅ Do not allow force pushes / deletions.

Para `develop`: al menos bloquear force push y borrado. (Sin required reviews:
trabajas solo; se activan cuando entre alguien más.)

## Integración con el backlog y los sprints

- Cada rama feature referencia una **HU o tarea** del backlog
  (`02-backlog-historias-de-usuario.md`).
- El estado de las HUs se actualiza en el propio backlog (checkbox) con un commit
  `docs(gestion): ...` — el repo es la fuente de verdad, nada vive fuera de git.
- El cierre de sprint sigue el criterio de fin definido en
  `03-plan-de-sprints.md` (por alcance, no por fechas).