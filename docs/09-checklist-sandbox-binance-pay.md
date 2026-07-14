# 09 — Checklist del sandbox de Binance Pay (HU-021/HU-022)

> Runbook para el día en que llegue el acceso al **merchant sandbox**. El
> adaptador, las firmas y la suite WireMock ya están construidos contra la
> especificación pública; esta lista valida todo contra el proveedor real y
> cierra lo que quedó bloqueado sin credenciales.

## 0. Prerrequisitos

- [ ] Cuenta merchant aprobada en el portal de Binance Merchant (el sandbox se
      habilita antes del KYB de producción, según docs/07).
- [ ] Credenciales del sandbox: API key, secret y clave pública RSA de webhooks.
- [ ] URL pública para recibir webhooks en local (túnel tipo ngrok/cloudflared)
      registrada en el panel del merchant.

## 1. Configuración (solo variables de entorno, nunca el repo)

```
BINANCE_BASE_URL=<url del sandbox>
BINANCE_API_KEY=<api key sandbox>
BINANCE_SECRET=<secret sandbox>
BINANCE_WEBHOOK_PUBLIC_KEY=<clave pública RSA (PEM o base64)>
```

En `application-local.properties`: descomentar el bloque binance,
`pasarela.proveedores.binance.habilitado=true` y
`pasarela.proveedores.simulado.habilitado=false` (UN solo adaptador activo:
`ProcesarWebhookService` inyecta un único `ProveedorDePagoPort` — ADR-003).

## 2. Validaciones de contrato (re-grabar fixtures)

- [ ] Crear una orden real en sandbox y comparar la respuesta con el fixture de
      `BinancePayAdapterContratoTest` (campos `qrContent`, `deeplink`, `prepayId`).
      Ajustar el fixture si difiere y anotar el cambio en el commit.
- [ ] **Decisión de denominación**: verificar si el sandbox acepta
      `currency=COP` directo. Si NO: la cotización COP→cripto la debe resolver
      el proveedor (REGLA DE ORO: nosotros no convertimos). Registrar la
      decisión como ADR-006 y ajustar `cuerpoDeCrearOrden`.
- [ ] Consultar la orden (`order/query`) pagada y sin pagar; comparar con los
      fixtures de reconciliación.
- [ ] Recibir un webhook real: verificar que los headers son
      `BinancePay-Timestamp/Nonce/Signature`, que la firma RSA valida con la
      clave configurada y que el payload coincide con el fixture de
      `BinancePayAdapterFirmasTest` (`bizIdStr`, `bizStatus`, `data`).

## 3. Pago E2E (criterio de cierre del sprint)

- [ ] Crear orden desde la API → QR en pantalla.
- [ ] Pagar con la app de Binance (cuenta de prueba del sandbox).
- [ ] Webhook real llega → orden en `PAGO_DETECTADO` → la caja ve **PAGADO ✓**
      por polling (ADR-005).
- [ ] Verificar en el diff del sprint que `dominio/` y `aplicacion/` quedaron
      intactos (valida ADR-003).

## 4. Caminos tristes contra el sandbox real (HU-022)

- [ ] Webhook con firma inválida → 401 + intento registrado.
- [ ] Webhook duplicado (reenviar desde el panel) → `DUPLICADO`, sin doble
      confirmación.
- [ ] Monto errado / orden inexistente → `PARA_REVISION` + bitácora.
- [ ] Pago tardío tras expiración → revisión manual con datos para reembolso.
- [ ] Caída del webhook: apagar el túnel, pagar, y verificar que la
      **reconciliación** converge sola (HU-015) con el evento `recon-*`.
