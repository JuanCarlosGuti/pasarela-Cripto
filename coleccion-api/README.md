# Colección de API — Bruno

Colección para probar el flujo completo de la pasarela con
[Bruno](https://www.usebruno.com/) (cliente API open source). Vive en el repo
como archivos de texto — misma filosofía que el resto del proyecto: **nada
vive fuera de git** (decisión en `docs/gestion/04-herramientas.md`).

## Cómo usarla

1. Instalar Bruno (gratis): https://www.usebruno.com/downloads
2. En Bruno: **Open Collection** → seleccionar esta carpeta (`coleccion-api/`).
3. Elegir el entorno **local** (esquina superior derecha).
4. Levantar el backend:
   ```bash
   docker compose up -d      # PostgreSQL
   ./mvnw spring-boot:run    # la app en localhost:8080
   ```

## Qué se puede probar hoy y qué viene

| Carpeta | Petición | Disponible |
|---|---|---|
| `00-salud` | Salud del sistema | ✅ **ya** (Sprint 0) |
| `01-autenticacion` | Login (JWT) | ⏳ Sprint 2 |
| `02-comercios` | Registrar / verificar comercio | ⏳ Sprint 2 |
| `03-ordenes` | Crear orden (QR), consultar, consulta pública | ⏳ Sprint 3 |
| `04-webhooks` | Webhook de pago del proveedor simulado | ⏳ Sprint 4 |

Las peticiones ⏳ documentan el **contrato tentativo** según el backlog
(`docs/gestion/02`): sirven de guía y se ajustan cuando cada endpoint se
implemente de verdad. La fuente de verdad del contrato final será el OpenAPI
(springdoc) del Sprint 3.

## El flujo encadenado

Las peticiones se pasan datos entre sí vía variables de entorno:

```
Login ──guarda──► {{token}}
Crear orden ──guarda──► {{ordenId}} y {{referenciaPago}}
Consultar orden ──usa──► {{ordenId}}
Webhook ──usa──► {{referenciaPago}}
```

Es decir, cuando todo esté implementado, el "demo" manual completo será:
login → crear orden → webhook de pago → consultar orden y verla en
`PAGO_DETECTADO`.

> ¿Postman? Bruno puede exportar la colección a formato Postman si algún día
> hace falta compartirla con alguien que use Postman.