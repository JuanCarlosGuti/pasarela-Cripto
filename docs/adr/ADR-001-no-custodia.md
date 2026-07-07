# ADR-001 — Sin custodia de fondos ni criptoactivos

**Estado:** Aceptado

## Contexto

La plataforma conecta a un pagador (con saldo cripto) con un comercio (que quiere
pesos). Existe la tentación de que la plataforma reciba la cripto, la convierta y
transfiera los pesos al comercio. Sin embargo:

- Custodiar cripto o dinero de terceros convierte a la empresa en un intermediario
  financiero, con una carga regulatoria muy pesada en Colombia (registro, supervisión,
  antilavado robusto).
- La SAS opera en **Régimen Simple de Tributación (RST)**, que tributa sobre **ingresos
  brutos**. Si el dinero de las ventas de los comercios pasara por las cuentas de la
  plataforma, la DIAN podría leerlo como ingreso bruto propio y se tributaría sobre
  dinero ajeno.
- El marco regulatorio cripto colombiano está en evolución; asumir custodia aumenta el
  riesgo ante cambios normativos.

## Decisión

**La plataforma NUNCA custodia fondos ni criptoactivos de terceros.** Actúa solo como
**capa de orquestación de software**. La custodia, la conversión cripto→COP y el KYC los
ejecuta un **proveedor de rampa de terceros ya regulado**, que **liquida los COP
directamente al comercio**. La plataforma solo registra, concilia y cobra su comisión.

## Consecuencias

**A favor:**
- Carga regulatoria mínima (proveedor tecnológico, no intermediario financiero).
- Compatible con el RST: el único ingreso propio es la comisión por servicio.
- Menor exposición ante cambios normativos.
- Diseño más simple: no hay que construir custodia, wallets ni monitoreo de cadena.

**En contra / restricciones:**
- **Requisito duro de diseño:** ningún flujo de dinero de terceros puede pasar por
  cuentas de la plataforma. Al elegir proveedor, se descarta cualquiera que obligue a
  recibir y redistribuir los fondos.
- Dependencia de un tercero para la conversión y liquidación (mitigado por ADR-003:
  puerto abstracto que permite múltiples proveedores).

## Notas

Esta decisión debe validarse con un abogado fintech antes de operar con dinero real
(gatillo previo al piloto). No bloquea el desarrollo técnico, que corre en paralelo.
