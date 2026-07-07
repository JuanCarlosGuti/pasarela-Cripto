# Documento para Inversionistas
## Pasarela de Pagos Cripto → COP para comercios en Colombia

> **Versión:** 1.0 — Documento de trabajo. Las cifras de mercado provienen de fuentes
> públicas citadas; las proyecciones propias están marcadas como tales y deben
> refinarse con datos del piloto.

---

## 1. Resumen ejecutivo

**El problema:** cerca de 6 millones de colombianos ya usan criptomonedas, pero casi
ningún comercio las acepta. Ese dinero no tiene dónde gastarse: los usuarios deben
hacer P2P, transferencias y malabares para convertirlo en pesos antes de comprar
cualquier cosa.

**La solución:** una pasarela que permite a cualquier comercio cobrar en cripto
escaneando un QR y **recibir pesos colombianos en su cuenta**, sin tocar
criptomonedas, sin volatilidad y sin conocimiento técnico. Tan fácil como cobrar
con Nequi.

**El modelo:** comisión porcentual por transacción (~2-2.5% al comercio, margen neto
~1-1.5% tras el costo del proveedor de conversión). Sin custodia de fondos: la
conversión y liquidación las ejecuta un proveedor regulado que paga directo al
comercio — lo que mantiene la carga regulatoria y el riesgo al mínimo.

**El momento:** Colombia es el quinto mercado cripto de América Latina, con
US$44.200 millones movidos en un año, y la presión para que los comercios acepten
criptoactivos "seguirá aumentando durante 2026" según la Cámara Colombiana de
Comercio Electrónico. La infraestructura (Binance Pay, rampas reguladas, stablecoins)
ya existe; falta quien la haga **accesible al comercio de barrio**. Ese es nuestro
espacio.

---

## 2. El problema

### Del lado del usuario cripto
- Tiene saldo en Binance o una wallet, pero **no puede gastarlo en tiendas**.
- Para usarlo debe: vender por P2P → esperar transferencia → recibir en Nequi/banco →
  ahí sí comprar. Fricción, tiempo y spread en cada paso.

### Del lado del comercio
- Sabe que hay clientes con cripto (y turistas/nómadas digitales que llegan sin pesos),
  pero no la acepta porque:
  - No quiere **volatilidad** (recibir un activo que puede caer 10% en un día).
  - No quiere **complejidad contable ni tributaria** (¿cómo declaro cripto?).
  - No quiere **riesgo legal** en un marco regulatorio aún en evolución.
  - No tiene **capacidad técnica** para integrar APIs de exchanges.

### El hueco del mercado
Las piezas existen por separado — exchanges con millones de usuarios, APIs de
conversión, stablecoins — pero **nadie las une con una experiencia simple para el
comercio pequeño y mediano colombiano**. Es el mismo patrón que Bold y Wompi
explotaron con los datáfonos: la tecnología existía; ganaron haciéndola trivial para
el tendero.

---

## 3. La solución

**Flujo en 20 segundos:** el comercio digita $50.000 en su celular → aparece un QR →
el cliente lo escanea desde su app de Binance (o wallet) y paga → el comercio ve
"PAGADO ✓" → los pesos llegan a su cuenta → el registro queda listo para su contador.

**Propuestas de valor:**

| Para el comercio | Para el pagador |
|---|---|
| Ventas nuevas que hoy pierde | Por fin gasta su cripto como dinero normal |
| Recibe **pesos**, nunca cripto | Paga en segundos desde la app que ya usa |
| Costo similar o menor al datáfono (2-2.5% vs. 2-4% de tarjetas) | Sin P2P, sin transferencias, sin spread oculto |
| Cero conocimiento técnico: QR + link de WhatsApp | Directorio de dónde pagar con cripto |
| Reportes listos para contador y DIAN | |

**Principio arquitectónico clave (y ventaja regulatoria):** la plataforma **nunca
custodia fondos**. Es software de orquestación: la conversión cripto→COP y la
liquidación al comercio las ejecuta un proveedor de rampa regulado. Esto reduce
drásticamente el riesgo regulatorio, simplifica el cumplimiento tributario y hace el
negocio más liviano en capital.

---

## 4. El mercado

### Tamaño y momentum (fuentes públicas)

- **~6 millones de colombianos** usan criptomonedas o plataformas relacionadas
  (Cámara Colombiana de Comercio Electrónico / Vurelo, 2026).
- **US$44.200 millones** en transacciones cripto en Colombia entre julio 2024 y junio
  2025 — **quinto mercado de América Latina**, tras Brasil, Argentina, México y
  Venezuela (Chainalysis).
- Colombia es el **#29 global** en adopción, escalando **siete posiciones** frente a
  2024 (Chainalysis 2025).
- América Latina creció **63% interanual** en adopción; las **stablecoins concentran
  ~90% del volumen** regional — el activo perfecto para pagos (sin volatilidad).
- En Colombia, **más de la mitad de las compras cripto terminan en stablecoins**
  ("dolarización digital"), en un contexto de inflación sobre la meta del banco central.
- **US$13.098 millones en remesas** recibió Colombia en 2025 (+10,6%, ~3% del PIB) —
  un flujo creciente que llega cada vez más por rieles cripto y necesita dónde gastarse.
- Bonus de demanda: **turismo y nómadas digitales** (especialmente Medellín) que llegan
  con cripto y sin pesos.

### TAM / SAM / SOM (estimación propia, a refinar con el piloto)

| Nivel | Definición | Lógica |
|---|---|---|
| **TAM** | Pagos presenciales y digitales de comercio en Colombia susceptibles de medios de pago electrónicos | Decenas de billones de COP anuales |
| **SAM** | Gasto potencial de los ~6M de usuarios cripto en comercios locales | Si solo el 5% de sus tenencias rotara anualmente en consumo local, hablamos de un mercado direccionable de cientos de millones de USD/año |
| **SOM (año 1-2)** | Comercios urbanos de nicho (tecnología, barberías, gastronomía joven, gimnasios, turismo) en Bogotá y Medellín | Objetivo inicial: capturar un volumen procesado (TPV) modesto pero demostrativo — ver plan comercial |

> Nota honesta para el inversionista: el mercado de *tenencia* cripto es enorme y
> medible; el mercado de *gasto* cripto en comercios es incipiente — esa es
> exactamente la oportunidad y también el riesgo. El piloto está diseñado para medir
> la conversión tenencia→gasto con datos reales.

---

## 5. Modelo de negocio

**Ingreso principal:** comisión por transacción.

```
Comisión al comercio:        ~2.0 – 2.5% por transacción
(–) Costo proveedor rampa:   ~1.0%
(=) Margen bruto plataforma: ~1.0 – 1.5%
```

**Comparable de referencia:** los datáfonos y pasarelas de tarjeta cobran al comercio
entre 2% y 4% más retenciones. No pedimos al comercio pagar algo nuevo: le damos un
medio de pago **adicional** al mismo costo de los que ya acepta.

**Ingresos secundarios (desde el día uno, financian el arranque):**
- Tarifa única de instalación/onboarding "cobro digital completo" (montamos su
  pasarela tradicional + la nuestra en una tarde).
- Futuro: suscripción por funcionalidades premium del dashboard (reportes avanzados,
  multi-sede), publicidad de posición en el directorio de comercios.

**Estructura de costos liviana:** sin custodia no hay tesorería, ni licencias
financieras pesadas, ni capital regulatorio. Costos = desarrollo, infraestructura
cloud (mínima en MVP), comercial/soporte, y asesoría legal puntual.

---

## 6. Competencia y posicionamiento

| Competidor | Qué hace | Por qué no nos mata |
|---|---|---|
| **Binance Pay directo** | Un comercio puede registrarse como merchant | Un tendero no navega documentación de APIs ni concilia en cripto; nosotros somos la capa que lo vuelve trivial + liquidación en COP + soporte local |
| **P2P (Binance P2P)** | El usuario convierte cripto a pesos | Resuelve la conversión del *usuario*, no el *cobro del comercio*; es fricción, no punto de venta |
| **Pasarelas tradicionales** (Wompi, Bold, PayU) | Tarjetas/PSE | No aceptan cripto; son nuestro comparable de precio y potenciales aliados o adquirentes |
| **Rampas/exchanges locales** (Bitso, Buda, etc.) | Conversión y trading | Son nuestros **proveedores**, no competidores directos en el punto de venta del comercio pequeño |
| **Procesadores cripto globales** (BitPay, etc.) | Checkout cripto para e-commerce | Enfocados en e-commerce global grande; sin presencia comercial ni soporte para el comercio físico colombiano |

**Nuestro foso (moat) inicial:** no es tecnología patentable — es **distribución y
experiencia local**: onboarding en minutos, soporte en español por WhatsApp,
reportes DIAN-friendly, presencia en la calle, y el efecto de red del directorio
"dónde pagar con cripto" (más comercios → más pagadores → más comercios).

---

## 7. Estado actual y tracción temprana

- ✅ Modelo de negocio y arquitectura definidos y documentados (sin custodia,
  validado contra el marco tributario del Régimen Simple).
- ✅ **Validación comercial iniciada: primeros comercios ya manifestaron interés**
  en implementar la plataforma (piden simplicidad de implementación — requisito ya
  incorporado al diseño del producto).
- ✅ Backend en desarrollo: arquitectura hexagonal Spring Boot, con documentación
  técnica completa (visión, arquitectura, dominio, pruebas, roadmap de 10 fases).
- ⏳ En curso: encuesta estructurada de validación a una base más amplia de comercios;
  solicitud de sandbox a Binance Pay y proveedores de rampa.
- ⏳ Próximo hito: MVP funcional en sandbox → piloto con 5-10 comercios reales en una
  zona concentrada.

---

## 8. Equipo

**Juan Carlos Gutiérrez Huérfano — Fundador**
- **Arquitecto de Software y Desarrollador Full Stack** con experiencia en soluciones
  empresariales bajo arquitectura hexagonal y DDD (Java/Spring Boot, Angular, AWS,
  Kubernetes), incluyendo plataformas con integraciones de pago y microservicios en
  compañías como Ceiba Software, SQDM y Thomas Instruments.
- **Administrador de Empresas de la Universidad Nacional de Colombia** — perfil dual
  técnico + negocio, poco común en fundadores solitarios.
- **7+ años como instructor SENA**: capacidad probada de enseñar, documentar y
  comunicar — el activo exacto que requiere la labor educativa de este mercado.
- Certificaciones en desarrollo backend, cloud y metodologías ágiles (Digital House,
  Scrum).

*El pitch del equipo en una línea:* la persona que puede **construir** la plataforma,
**entender** el negocio y **educar** al mercado es la misma — velocidad y coherencia
de ejecución que un equipo fragmentado no tiene en esta etapa.

---

## 9. Riesgos y mitigación (transparencia total)

| Riesgo | Realidad | Mitigación |
|---|---|---|
| **Regulatorio** | El marco cripto colombiano está en evolución (DIAN ya exige reportes a plataformas; hay proyecto de ley en trámite) | Modelo sin custodia = mínima superficie regulatoria; asesoría legal antes de dinero real; arquitectura adaptable |
| **Dependencia de proveedores** | Si Binance/rampa corta el servicio, el negocio se detiene | Puerto de proveedor abstracto: multi-proveedor por diseño; segundo riel planificado |
| **Adopción más lenta de lo esperado** | El gasto cripto en comercios es incipiente | Piloto barato y medible antes de escalar; ingreso secundario (instalaciones) financia la espera; educación embebida en el producto |
| **Competencia de un grande** | Una pasarela establecida podría añadir cripto | Ser primeros en el nicho desatendido + relación directa con comercios = posición de adquisición atractiva, no amenaza existencial |
| **Fundador único** | Riesgo de bus factor | Documentación exhaustiva desde el día uno; plan de incorporar talento comercial con la inversión |

---

## 10. Uso de fondos (propuesta a discutir)

> Los montos exactos se definirán según la ronda; esta es la estructura de prioridades.

1. **Comercial y adquisición de comercios (40-50%)** — la tecnología ya avanza; el
   cuello de botella será la calle: perfil comercial, materiales, incentivos de
   onboarding (primer mes gratis).
2. **Producto y desarrollo (25-30%)** — completar MVP, segundo riel (on-chain),
   app/PWA pulida, seguridad.
3. **Legal y cumplimiento (10-15%)** — asesoría fintech, contratos, adecuación ante
   la regulación entrante.
4. **Operación y soporte (10-15%)** — soporte a comercios del piloto, infraestructura,
   contabilidad.

**Hitos que compra la inversión:** MVP en producción → piloto validado con métricas →
50-100 comercios activos → datos para ronda siguiente o rentabilidad temprana.

---

## 11. La visión a 3 años

Ser **la red de aceptación cripto del comercio colombiano**: miles de comercios donde
cualquier persona con cripto paga como con cualquier otro medio, un directorio que la
comunidad consulta para decidir dónde comprar, y la infraestructura lista para
expandirse a mercados vecinos con dinámica similar (Perú, Ecuador, Centroamérica) —
o para ser el socio/adquisición natural de una pasarela tradicional que quiera entrar
al mundo cripto con la red ya construida.
