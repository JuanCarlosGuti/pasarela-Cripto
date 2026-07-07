# ADR-004 — Idempotencia estricta en webhooks

**Estado:** Aceptado

## Contexto

Los proveedores de pago confirman las transacciones mediante **webhooks**. Por la
naturaleza de las redes, un webhook puede:

- Llegar **más de una vez** (el proveedor reintenta si no recibe confirmación a tiempo).
- **Perderse** (caídas de red, timeouts).
- Llegar **fuera de orden**.

Si el sistema procesara dos veces el mismo evento, podría **confirmar o liquidar un pago
dos veces** — un error grave con dinero de por medio.

## Decisión

Todo procesamiento de webhook es **idempotente y robusto**, con estas reglas obligatorias:

1. **Validar la firma** del webhook antes de procesarlo.
2. **Guardar el evento crudo** (`EventoProveedor`) antes de procesarlo.
3. **Clave de idempotencia** `(proveedor, idExternoEvento)` con **restricción de
   unicidad en base de datos**. Si el evento ya fue procesado, la operación es un no-op
   que responde `200 OK`.
4. **Job de reconciliación** de respaldo que consulta al proveedor el estado de las
   órdenes atascadas, de modo que el sistema converja al estado correcto **aunque un
   webhook nunca llegue**.

Procesar el mismo evento N veces produce **exactamente el mismo resultado** que
procesarlo una vez.

## Consecuencias

**A favor:**
- Elimina el riesgo de doble cobro / doble liquidación.
- El sistema es robusto ante reintentos, pérdidas y desorden de webhooks.
- Trazabilidad completa (evento crudo guardado) para auditoría y disputas.

**En contra:**
- Más complejidad en el procesamiento (validación de firma, persistencia previa,
  control de unicidad, job de reconciliación). Es complejidad **esencial**, no
  accidental: el dominio del problema lo exige.

## Notas

Esta es una de las áreas con **mayor exigencia de pruebas** del proyecto: firma
válida/inválida, evento duplicado, orden inexistente, pago tardío y monto errado deben
estar cubiertos (ver Estrategia de Pruebas).
