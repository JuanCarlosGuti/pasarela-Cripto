# ADR-006 — Flujo del dinero: riel de cobro vs. conversión y liquidación

**Estado:** Aceptado (modelo). La selección del proveedor concreto está en curso
(T-007, `docs/negocio/04-seleccion-proveedor-rampa.md`) y actualizará este ADR.

## Contexto

Al iniciar el trámite de la cuenta merchant de Binance Pay (Sprint 7) quedó en
evidencia una ambigüedad que `docs/07` había aplazado como "detalle a cerrar en
la fase de integración": **Binance Pay liquida cripto a la cuenta merchant de
Binance de quien recibe**. Si la plataforma fuera "el merchant" que recibe todos
los pagos, custodiaríamos fondos ajenos (violación de ADR-001); si cada comercio
fuera su propio merchant, recibiría cripto — y la promesa de valor del MVP es
que el comercio **recibe COP en su cuenta (Nequi/banco) sin tocar cripto**.

La confusión nace de mezclar dos piezas que son independientes:

1. **El riel de cobro:** por dónde paga el pagador (Binance Pay, wallet
   on-chain, etc.).
2. **La conversión y liquidación:** quién convierte la cripto a COP y deposita
   en la cuenta del comercio.

## Decisión

1. **La promesa de valor del MVP la cumple la pieza 2**, y la ejecuta un
   **proveedor de rampa regulado** que recibe la cripto del pagador, convierte,
   y liquida COP **directamente en la cuenta del comercio final** (jamás en
   cuentas de la plataforma — ADR-001). Este proveedor es el **adaptador
   principal** de `ProveedorDePagoPort` para el piloto.
2. **Binance Pay directo queda como riel complementario**, no como el camino de
   la promesa: sirve para comercios cripto-friendly que acepten recibir cripto
   en su propia cuenta merchant de Binance (la plataforma como "socio de canal"
   facilita el registro). El `BinancePayAdapter` (HU-021) conserva su valor para
   este segmento y como validación técnica del puerto.
3. **Prohibido el modelo "recibir todo y repartir"**: registrar a la SAS como
   merchant receptor de los pagos de todos los comercios queda descartado de
   plano (custodia + intermediación financiera + RST, ver ADR-001).
4. **Nuestra comisión se cobra por fuera del flujo de fondos del comercio**:
   por facturación al comercio, o por split en origen del proveedor si lo
   soporta (el proveedor nos paga NUESTRA comisión directamente; el dinero del
   comercio nunca pasa por nosotros). La modalidad se cierra con el proveedor
   elegido.

## Criterios de selección del proveedor de rampa (requisitos de T-007)

| Criterio | Umbral |
|---|---|
| Liquidación COP directa al comercio final (Nequi/banco) | **Obligatorio** |
| Alta de sub-comercios vía API con KYB del proveedor | **Obligatorio** |
| Costo todo-en (fee + spread vs TRM + payout) | **≤ 1.5%** para margen ≥1% con comisión ≤2.5% |
| Dirección de depósito única por orden + webhook firmado | Requerido por nuestro flujo (docs/07) |
| USDT/USDC en redes baratas (Tron/BSC/Polygon) | Requerido (tickets pequeños) |
| Operación regulada en Colombia (SFC/UIAF, SARLAFT) | Obligatorio |

Candidatos identificados (estado del arte + verificación jul-2026): Koywe,
Mural Pay, Bitso Business, Minteo (COPM), Cobre. Payválida demuestra que el
modelo completo ya opera comercialmente en Colombia.

**Resultado de la investigación verificada (2026-07-14, detalle y citas en
`docs/negocio/04-seleccion-proveedor-rampa.md`):** Mural Pay es el candidato
principal — único con las tres patas verificadas (payout COP a terceros,
sub-comercios "Organizations" con KYB del proveedor y fondos a nombre del
comercio, webhooks firmados). Bitso Business es el respaldo (payout COP a
terceros verificado). El costo todo-en no es público en ningún proveedor:
se resuelve con cotización escrita en demo comercial.

## Consecuencias

- **El camino crítico del piloto (Sprint 8) es T-007**, no el sandbox de
  Binance: sin proveedor de rampa contratado no hay promesa de valor completa.
- Cuando se elija proveedor, se construye su adaptador del **mismo puerto**
  (`ProveedorDePagoPort`) — cero cambios en dominio y aplicación (ADR-003).
  Todo lo construido (webhooks idempotentes, reconciliación, liquidación,
  dashboard) sirve tal cual.
- **Riesgo principal: margen.** Si el costo todo-en supera 1.5%, opciones en
  orden: negociar por volumen proyectado, subir la comisión inicial del
  comercio (sigue siendo competitiva vs. datáfono), cambiar de proveedor.
- El trámite de la cuenta merchant propia de Binance baja a prioridad media:
  cuenta de desarrollo para HU-021 y para el riel complementario.
