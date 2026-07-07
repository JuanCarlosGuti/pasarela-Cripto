# 01 — Visión y Alcance del MVP

## Visión del producto

Ser la forma **más simple** en que un comercio colombiano acepta pagos en cripto y
recibe pesos, sin aprender nada nuevo y sin asumir riesgos. La analogía guía: *"tan
fácil como cobrar con Nequi, pero para cripto"*.

El diferencial no es la conversión (esa la hace un tercero), sino la **experiencia
completa para el comercio**: onboarding en minutos, cobro por QR, liquidación en
pesos, y reportes listos para el contador y la DIAN.

---

## Alcance del MVP

El MVP se centra en **un solo caso de uso, con un solo riel de pago**, para validar el
negocio con la mínima complejidad posible.

### Caso de uso del MVP

> Un comercio genera un cobro en COP → se muestra un QR → el pagador paga con su saldo
> de **Binance** (vía Binance Pay) → el proveedor confirma el pago → el comercio ve
> "PAGADO" y recibe pesos → la plataforma registra la comisión.

### DENTRO del alcance del MVP ✅

- **Onboarding de comercios** con verificación manual (aprobación desde panel admin).
- **Autenticación** con roles `ADMIN` y `COMERCIO` (Spring Security + JWT).
- **Creación de orden de pago** en COP con generación de QR y expiración.
- **Un solo riel de pago: Binance Pay** (en sandbox primero, luego producción).
- **Recepción y procesamiento de webhooks** con validación de firma e idempotencia.
- **Job de reconciliación** para órdenes cuyo webhook no llegó.
- **Registro de liquidación y conciliación** (el proveedor liquida directo al comercio;
  la plataforma registra y concilia).
- **Cálculo y registro de la comisión** de la plataforma.
- **Dashboard del comercio:** ventas del día/mes, historial exportable a CSV/Excel,
  comprobante por transacción.
- **Controles básicos de cumplimiento:** límites por transacción y por comercio/mes,
  bitácora de operaciones inusuales.

### FUERA del alcance del MVP ❌ (para después)

- Riel on-chain (USDT/USDC vía proveedor de rampa) — *segundo adaptador, post-MVP*.
- Módulo P2P (persona a persona).
- Aplicación móvil nativa para el comercio.
- Múltiples proveedores de rampa simultáneos.
- Directorio público de comercios ("dónde pagar con cripto").
- Programa de referidos / fidelización.
- Múltiples monedas de liquidación distintas a COP.
- Microservicios (el MVP es un monolito modular).
- Custodia propia de fondos o cripto (**nunca**, ver ADR-001).

---

## Por qué solo Binance Pay en el MVP

- Es el riel que cubre a la **mayoría de usuarios cripto colombianos** (Binance es el
  exchange de mayor volumen en el país).
- Transferencia **off-chain interna de Binance**: instantánea, sin comisión de red, sin
  esperar confirmaciones de blockchain, sin riesgo de monto errado.
- La **integración más simple** de todos los rieles → menor tiempo al primer piloto.
- Diseñamos el puerto `ProveedorDePagoPort` de forma que añadir el riel on-chain
  después sea **un adaptador nuevo**, no una reescritura.

---

## Criterios de éxito del MVP

El MVP se considera exitoso si, en el piloto con 5-10 comercios reales, se cumple:

| Métrica | Objetivo |
|---------|----------|
| Onboarding de un comercio | < 10 minutos, sin ayuda técnica |
| Tasa de éxito de pagos | > 95% de los intentos completan |
| Tiempo QR → confirmación | Segundos (off-chain Binance) |
| Tiempo confirmación → COP en cuenta | Según proveedor, medido y transparente |
| Doble cobro / doble liquidación | **Cero** (idempotencia estricta) |
| Tickets de soporte por transacción | Tendencia decreciente |

---

## Supuestos y dependencias

- **Supuesto:** el proveedor de rampa/Binance liquida COP **directamente al comercio**;
  la plataforma nunca recibe esos fondos. (Requisito por ADR-001 y Régimen Simple.)
- **Dependencia externa:** acceso a Binance Pay Merchant (sandbox primero).
- **Dependencia legal:** validación con abogado fintech antes de mover dinero real
  (gatillo previo al piloto, no bloquea el desarrollo técnico).

---

## Riesgos conocidos

| Riesgo | Mitigación |
|--------|-----------|
| Cambio regulatorio (marco cripto en evolución) | Diseño sin custodia; adaptadores intercambiables; vigilancia normativa |
| Dependencia de un solo proveedor | Puerto abstracto; segundo proveedor diseñado desde el inicio |
| Pérdida de webhooks | Job de reconciliación + polling de respaldo |
| Fricción en onboarding | Verificación manual al inicio; iterar con feedback del piloto |

---

## Fuera de discusión (invariantes del producto)

1. La plataforma **nunca custodia** fondos ni cripto.
2. Ningún dinero de terceros pasa por cuentas de la plataforma.
3. El comercio **nunca** necesita entender de cripto para usar el producto.
4. Cada peso cobrado queda **conciliado y auditable**.
