# Negocio 04 — Selección del proveedor de rampa (T-007)

> **Por qué existe este documento:** la promesa de valor del MVP ("el comercio
> cobra cripto y recibe COP en su cuenta") la cumple un **proveedor de rampa
> regulado** que convierte y liquida directo al comercio (ADR-001, ADR-006).
> Este documento registra la evaluación de candidatos, la evidencia verificada
> y el proceso de selección. Última actualización: **2026-07-14** (investigación
> profunda con verificación adversarial sobre fuentes públicas del 2026-07-13).

---

## 1. Requisitos (de ADR-006)

| Criterio | Umbral |
|---|---|
| Liquidación COP directa al comercio final (banco/Nequi) | **Obligatorio** — REGLA DE ORO |
| Alta de sub-comercios vía API con KYB del proveedor | **Obligatorio** |
| Costo todo-en (fee + spread vs TRM + payout) | **≤ 1.5%** para margen ≥1% con comisión ≤2.5% |
| Dirección de depósito única por orden + webhook firmado | Requerido por nuestro flujo (docs/07) |
| USDT/USDC en redes baratas (Tron/BSC/Polygon) | Requerido (tickets pequeños) |
| Operación regulada en Colombia (SFC/UIAF, SARLAFT) | Obligatorio |

## 2. Veredicto por proveedor (estado de la evidencia)

| Proveedor | Liquida COP a terceros | Sub-comercios por API | Cobro + webhooks firmados | Veredicto |
|---|---|---|---|---|
| **Mural Pay** | ✅ Verificado (3-0) | ✅ Verificado (3-0) — "Organizations" con KYB de Mural | ✅ Verificado (3-0) — Payins COP + ECDSA | **PRINCIPAL** |
| **Bitso Business** | ✅ Verificado (2-0) — "Withdrawing COP to a Third Party" | ⚠️ Señales sin verificar | ⚠️ Juno sin firma documentada (señal) | **RESPALDO** |
| **Koywe** | ⚠️ Señal: sí, pero desde saldo virtual de la plataforma (🚨 posible choque con REGLA DE ORO) | ⚠️ Señal: sí | ⚠️ Señal: dirección única por orden en offramp | Tercer candidato, re-verificar |
| **Cobre** | ⚠️ Señal: sí vía Bre-B/FastPay/ACH | ⚠️ Counterparties con llaves Bre-B | ⚠️ Señal: HMAC-SHA256, pero **sin cobro cripto entrante** | Riel de liquidación, no rampa completa |
| **Minteo (COPM)** | ⚠️ Señal: emisor de stablecoin, no procesador de liquidación fiat | — | — | No encaja como rampa |

**Leyenda:** ✅ = sobrevivió verificación adversarial (3 verificadores independientes).
⚠️ = señal de fuentes primarias que quedó sin verificar (fallo de infraestructura del
verificador); tratar como hipótesis.

## 3. Evidencia clave verificada (con cita)

- **Mural Pay liquida COP a cuentas de terceros**: riel `cop` documentado como
  *"COP sent to bank accounts in Colombia"*; el ejemplo oficial del payout lleva
  banco, titular, cuenta y cédula del beneficiario, separados de la cuenta de la
  plataforma. Lotes de hasta 350 payouts a Bancolombia/Davivienda/Nequi/Daviplata.
  — [Create a Payout Request](https://developers.muralpay.com/docs/create-a-payout-request) ·
  [API completa](https://raw.githubusercontent.com/muralpay/developer-resources/refs/heads/main/mural-api-documentation-complete.md)
- **Modelo sub-merchant de Mural ("Organizations")**: *"An Organization represents
  your end users—the individuals or businesses who own the funds for which you are
  facilitating payments"*; KYB hosted por Mural; cuenta virtual automática al
  aprobar. **Los fondos quedan a nombre del comercio, nunca de la plataforma** —
  encaje directo con ADR-001. — [Business KYC Requirements](https://developers.muralpay.com/docs/business-kyc-requirements)
- **Cobro y webhooks de Mural**: Payins API para COP (links PSE/Nequi); el pay-in
  cripto entra por transferencia a la wallet de la cuenta (⚠️ **sin dirección única
  por orden confirmada** — pregunta de demo); webhooks firmados ECDSA/SHA256 sobre
  `{timestamp}.{cuerpo}`, header canónico `x-mural-webhook-signature`.
  — [Payins](https://developers.muralpay.com/docs/payins) ·
  [Signature Validation](https://developers.muralpay.com/docs/signature-validation)
- **Bitso Business**: endpoint `api/v3/withdrawals` con `third_party_withdrawal=true`
  y campos del beneficiario con tipos de documento colombianos (CC, CE, NIT, PASS,
  TI, RC). — [Withdrawing COP to a Third Party](https://docs.bitso.com/bitso-payouts-funding/docs/getting-started)
- **Pricing: REFUTADO (0-3)** el único número público (~1.75% leído del ejemplo de
  Mural): ese ejemplo no representa el pricing efectivo. **Ningún proveedor publica
  costo todo-en** → solo se resuelve con cotización escrita en la demo.
- **Validación del mercado**: Payválida ya opera cripto→COP al banco del comercio en
  Colombia (300+ wallets, incl. Binance Pay); no revela su infraestructura de
  conversión. — [Colombia Fintech](https://colombiafintech.co/2025/07/10/abre-tu-negocio-al-futuro-financiero-con-el-boton-cripto-monedas-de-payvalida/)

## 4. Lo que NO se pudo responder con fuentes públicas (va a las demos)

1. **Costo todo-en real** (fee + spread FX + payout) para volúmenes de PSP — pedirlo
   **por escrito** sobre un ticket de $50.000 COP.
2. **Atribución del pago cripto entrante por orden** en Mural (¿dirección única por
   orden? ¿conciliación por monto?).
3. **Figura regulatoria en Colombia** de cada proveedor (SFC/UIAF/SARLAFT) y si sus
   términos permiten el modelo agregador/PSP sin custodia.
4. Tiempos reales del KYB de sub-comercios (el Knowledge Base de Mural sugiere
   24-48h, pero para clientes directos).

## 5. Checklist para las demos (las 6 preguntas + 3 decisivas)

¿Liquidan directo al comercio final? · ¿Alta de sub-comercios por API con su KYB y
en cuánto tiempo? · ¿Costo todo-en para ticket de $50.000 COP por escrito? ·
¿Dirección de depósito única por orden + webhook firmado? · ¿Qué redes/monedas? ·
¿Bajo qué figura regulatoria operan en Colombia? · **+ Mural:** ¿"Organizations"
garantiza jurídicamente que los fondos nunca pasan por la plataforma? · ¿Tienen
otros clientes tipo pasarela/PSP? · ¿Sus términos permiten agregador a escala?

## 6. Guion de contacto

> **Asunto:** Pasarela de pagos en Colombia — evaluación de [proveedor] como
> proveedor de conversión y liquidación COP
>
> Hola, soy Juan Carlos Gutiérrez, de Crear Code César S.A.S (Medellín, Colombia).
> Estamos construyendo una pasarela que permite a comercios físicos pequeños cobrar
> en cripto y recibir pesos colombianos directamente en su cuenta (Nequi o banco),
> sin custodiar nunca fondos de terceros. El backend está construido y operando
> contra un proveedor simulado; estamos seleccionando el proveedor de rampa para el
> piloto con comercios reales en Medellín. Quisiera agendar una demo
> técnica/comercial de 30-45 minutos. [+ las preguntas del checklist §5]

Canales: Mural Pay → muralpay.com "Contact Sales" · Bitso Business →
bitso.com/business formulario · Koywe → koywe.com "Contact/Sales".

## 7. Próximos pasos

- [ ] Re-verificar las 19 afirmaciones pendientes de Koywe/Cobre/Minteo (en curso).
- [ ] Enviar contacto a Mural Pay (principal) y Bitso Business (respaldo).
- [ ] Demos con cotización todo-en por escrito.
- [ ] Actualizar ADR-006 con el proveedor elegido y el mecanismo de comisión.
- [ ] Solo entonces: construir el adaptador del proveedor (mismo puerto, ADR-003).
