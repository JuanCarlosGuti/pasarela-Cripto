# 03 — Guía de Estilo y Clean Code

> Inspirada en la cultura de ingeniería de Ceiba Software: código limpio, pruebas
> primero (TDD), diseño orientado al dominio y respeto estricto por las fronteras
> arquitectónicas.

---

## Principios rectores

1. **El código se lee muchas más veces de las que se escribe.** Prioriza claridad
   sobre astucia.
2. **El dominio manda.** El código de negocio debe leerse como el negocio habla
   (lenguaje ubicuo, en español).
3. **Primero la prueba.** TDD: red → green → refactor. Sin prueba no hay código.
4. **Una responsabilidad por unidad.** Clases, métodos y casos de uso pequeños y
   enfocados (SRP, la "S" de SOLID).
5. **Dependencias hacia el dominio.** Nunca al revés (ver Arquitectura).

---

## Nombrado

- **Dominio en español**, alineado al negocio: `OrdenDePago`, `Comercio`,
  `calcularComision()`, `estaExpirada()`.
- **Nombres reveladores de intención.** `orden.marcarComoPagada()` mejor que
  `orden.setEstado(2)`.
- **Sin abreviaturas crípticas.** `referenciaPago`, no `refPg`.
- **Booleanos como preguntas.** `estaExpirada`, `puedeConfirmarse`, `tienePagoDetectado`.
- **Clases = sustantivos; métodos = verbos.** `Liquidacion` (clase),
  `liquidar()` (método).
- **Constantes en MAYÚSCULA_CON_GUIONES.** `MINUTOS_EXPIRACION_ORDEN = 15`.

## Value Objects sobre tipos primitivos

Evita "obsesión por primitivos". Un monto no es un `BigDecimal` suelto: es `Dinero`.

```java
// ❌ Evitar
public void crearOrden(BigDecimal monto, String moneda) { ... }

// ✅ Preferir
public void crearOrden(Dinero monto) { ... }
```

`Dinero` encapsula monto + moneda, valida que no sea negativo, y ofrece operaciones
seguras. Esto elimina clases enteras de errores y hace el código auto-documentado.

## Métodos y funciones

- **Cortos.** Si un método no cabe en la pantalla, probablemente hace demasiado.
- **Un nivel de abstracción por método.** No mezcles lógica de alto nivel con detalles.
- **Pocos parámetros.** 0-2 ideal, 3 aceptable. Más de 3 → agrupa en un objeto.
- **Sin efectos secundarios ocultos.** Un método llamado `calcularComision()` no debe
  además guardar en base de datos.
- **Evita banderas booleanas como parámetro.** `procesar(true)` no dice nada; separa en
  dos métodos con nombre.

## Objetos y estructuras

- **Encapsula el estado.** Nada de setters públicos en las entidades de dominio; el
  estado cambia mediante métodos con significado de negocio
  (`orden.confirmarPago(evento)`), que protegen las invariantes.
- **Constructores que garantizan objetos válidos.** Si un objeto no puede existir en
  estado inválido, no habrá que validarlo por todas partes.
- **Inmutabilidad donde se pueda,** especialmente en Value Objects (usa `record` de Java).

## Manejo de errores

- **Excepciones de dominio con significado**, no genéricas:
  `OrdenNoPuedeConfirmarseException`, no `RuntimeException`.
- **Falla rápido.** Valida las precondiciones al inicio del método.
- **No uses excepciones para flujo de control normal.** Un pago que expira es un estado
  esperado, no una excepción.
- **Nunca tragues excepciones** (`catch` vacío). Si no puedes manejarla, propágala.
- **No expongas detalles internos** en las respuestas de error de la API.

## Comentarios

- **El mejor comentario es un buen nombre.** Comenta el *por qué*, no el *qué*.
- **Evita comentarios que repiten el código.** `// incrementa i` sobra.
- **Documenta las decisiones no obvias** y las reglas de negocio sutiles.
- **Sin código comentado.** Para eso está Git.

## Formato y consistencia

- Sigue las convenciones estándar de Java (Google Java Format o el formateador de
  IntelliJ por defecto están bien).
- Imports ordenados, sin comodines (`import java.util.*` ❌).
- Una clase pública por archivo.
- Configura el formateo automático al guardar en el IDE para no discutir estilo en
  los code reviews.

---

## SOLID en la práctica

| Principio | Aplicación en este proyecto |
|-----------|------------------------------|
| **S** — Responsabilidad única | Un caso de uso por clase de aplicación |
| **O** — Abierto/cerrado | Añadir un proveedor = nuevo adaptador, sin tocar el existente |
| **L** — Sustitución de Liskov | Cualquier `ProveedorDePagoPort` es intercambiable |
| **I** — Segregación de interfaces | Puertos pequeños y específicos, no interfaces gigantes |
| **D** — Inversión de dependencias | El dominio define puertos; la infra los implementa |

---

## Reglas específicas del proyecto

- El **dominio no importa** `org.springframework.*` ni `jakarta.persistence.*`.
- Las **entidades JPA viven en infraestructura**, separadas de las de dominio, con
  mappers entre ambas.
- `@Transactional` va en la **capa de aplicación**, nunca en el dominio.
- Toda operación que cambie estado de una `OrdenDePago` pasa por un **método de dominio**
  que valida la transición (nunca `setEstado` directo).
- Los **secretos y credenciales** van en variables de entorno, jamás en el código ni en
  el repositorio.
- Todo **webhook** valida firma y es idempotente (ver doc de proveedores).

---

## Definición de "Terminado" (Definition of Done)

Una tarea está terminada cuando:

- [ ] Tiene pruebas (unitarias del dominio, y de integración si toca infraestructura).
- [ ] Todas las pruebas pasan (`./mvnw verify` en verde).
- [ ] Respeta las fronteras arquitectónicas (ArchUnit en verde).
- [ ] No baja la cobertura ni introduce *code smells* críticos (SonarQube).
- [ ] Los nombres reflejan el dominio y son claros.
- [ ] Maneja los caminos tristes relevantes (pago tardío, doble pago, etc.).
- [ ] No hay secretos hardcodeados.
- [ ] El diff fue revisado (por ti o en code review).
