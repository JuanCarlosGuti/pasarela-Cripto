# ADR-003 — Proveedores de pago como adaptadores de un puerto

**Estado:** Aceptado

## Contexto

El MVP usa un solo riel de pago (Binance Pay), pero el negocio necesitará más rieles
(on-chain vía rampa, otros proveedores) y redundancia ante fallos de un proveedor.
Además, existe el riesgo de que un proveedor corte el servicio, lo que detendría el
negocio si el sistema estuviera acoplado a él.

## Decisión

Definir un **puerto de salida `ProveedorDePagoPort`** en el dominio, que abstrae las
operaciones de cualquier riel de pago (crear cobro, validar firma de webhook,
interpretar webhook). Cada proveedor concreto se implementa como un **adaptador** en la
capa de infraestructura.

El dominio y la aplicación dependen solo del puerto, nunca de un proveedor concreto.

## Consecuencias

**A favor:**
- Añadir un proveedor = crear un adaptador nuevo, sin tocar el dominio (principio
  abierto/cerrado).
- Cambiar de proveedor o tener respaldo es cuestión de configuración.
- El dominio queda testeable con un adaptador falso, sin depender de accesos externos.
- Resiliencia: si un proveedor falla, se puede enrutar a otro.

**En contra:**
- Una capa de abstracción más (mapeo entre el modelo del proveedor y el del dominio).
  Es el costo esperado y justificado de la flexibilidad.

## Notas

En el MVP se implementa primero un **adaptador simulado** (para desarrollar sin el
sandbox) y luego el `BinancePayAdapter` real. El adaptador on-chain llega post-MVP como
una segunda implementación del mismo puerto.
