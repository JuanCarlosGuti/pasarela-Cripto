# Estado del Arte
## Pagos con Cripto en Comercios: Colombia, Latinoamérica y el mundo

> Mapa de las soluciones existentes y en gestación en el nicho de "gastar cripto en
> comercios", el hueco real del mercado y las implicaciones estratégicas para el
> proyecto. Fuentes públicas citadas por nombre; corte de información: julio de 2026.
>
> **Versión 1.0 — Documento de trabajo · Confidencial**

---

## 1. Resumen ejecutivo

El nicho existe, crece rápido y está en plena efervescencia — lo cual valida la
necesidad, pero también significa que no estamos solos. Los hallazgos centrales:

- El mercado global de pasarelas cripto alcanzó **~US$1.200 millones en 2024** y se
  proyecta creciendo **~21% anual hasta 2034** (ChangeHero/industria). Los gigantes de
  pagos (Stripe, PayPal, Shopify, Visa, Mastercard) ya entraron, pero concentrados en
  e-commerce de EE.UU./Europa — no en el comercio físico pequeño latinoamericano.
- La tendencia dominante son las **stablecoins, no Bitcoin**: en LatAm superan el 90%
  del volumen cripto (Chainalysis), y el patrón ganador de 2025-2026 es conectar
  stablecoins con los rieles de pago nacionales (Pix en Brasil, Transferencias 3.0 en
  Argentina, **Bre-B en Colombia**).
- **En Colombia ya hay actividad:** Binance Pay reportó 500+ locales, la DIAN
  contabilizó 680 establecimientos que aceptan cripto, existen pasarelas con
  liquidación en COP (Payválida), stablecoins de peso colombiano (COPW de
  Bancolombia/Wenia, COPM de Minteo) e iniciativas de adopción (Trokera, Satoshi
  Team). Pero **nadie domina la última milla del comercio físico pequeño** con
  experiencia local, soporte y modelo de acompañamiento.
- **La conclusión estratégica:** la infraestructura ya existe (no hay que construirla,
  hay que orquestarla — exactamente nuestro modelo). La ventana de oportunidad es la
  distribución y la experiencia local, y se está cerrando a velocidad media:
  **12-24 meses** antes de que un jugador grande o bien financiado ocupe el espacio
  del comercio físico colombiano.

---

## 2. Nivel global: el estado del arte mundial

### 2.1 Los gigantes ya entraron (2025-2026, punto de inflexión)

| Jugador | Qué hace | Alcance / límite |
|---|---|---|
| **Stripe** | Desde dic. 2025, todo merchant acepta USDC/USDB en checkout estándar sin cambios de código; liquida en USD; comisión plana ~1,5% | Solo negocios de EE.UU. por ahora (según su propia documentación) |
| **PayPal** | "Pay with Crypto": acepta 100+ criptos y convierte a PYUSD o fiat; ~70 mercados | Enfocado en e-commerce y su propio ecosistema |
| **Shopify** | Checkout con USDC (con Coinbase y Stripe) en mercados elegibles de Europa y Hong Kong | Elegibilidad limitada; e-commerce, no tienda física |
| **Coinbase Commerce / Payments** | Stack completo de aceptación stablecoin sobre su red Base; apunta a PSPs y grandes empresas | B2B/enterprise, no comercio pequeño |
| **BitPay** | El veterano (2011): acepta cripto con liquidación en fiat, cripto o mixta; casos como AMC Theatres | Liquidación fiat centrada en USD/EUR/GBP; sin presencia comercial LatAm |
| **Binance Pay** | Pagos off-chain instantáneos entre usuarios Binance y merchants; QR y API | Riel excelente (el que usaremos) pero sin "última milla" local: el comercio debe autogestionarse |
| **Visa / Mastercard** | Programas de tarjetas cripto: el usuario gasta cripto en cualquier datáfono; Mastercard habla de 150M de puntos de aceptación | Resuelven el lado del pagador vía rieles de tarjeta; el comercio paga la comisión de siempre |
| **NOWPayments, Cryptomus, B2BinPay, Triple-A, BVNK, TransFi** | Pasarelas cripto globales para e-commerce: 300+ monedas, plugins, APIs | Comercio online internacional; sin liquidación COP nativa ni soporte local |

### 2.2 Las dos rutas que compiten por el mismo problema

- **Ruta A — Tarjeta cripto (rieles Visa/MC):** el usuario obtiene una tarjeta
  (Lemon, Oobit, Binance Card, Crypto.com) que descuenta de su saldo cripto y paga al
  comercio en moneda local por el datáfono normal. *Ventaja:* aceptación universal
  inmediata. *Desventaja:* el comercio paga la comisión de tarjeta de siempre, no hay
  relación comercio-plataforma, y el usuario suele pagar spreads/cuotas.
- **Ruta B — Pago directo QR (nuestra ruta):** el comercio muestra un QR propio y el
  pago viaja por rieles cripto/stablecoin con conversión a fiat. *Ventaja:* comisión
  potencialmente menor, relación directa con el comercio, datos y servicios de valor
  agregado. *Desventaja:* hay que construir la red de aceptación comercio por comercio.

**Tendencia 2026:** la ruta B está ganando tracción cuando se conecta con los rieles
de pago instantáneo nacionales (ver LatAm), porque combina la aceptación masiva de la
ruta A con los costos bajos de la B.

### 2.3 Señales regulatorias globales

EE.UU. formalizó el sector (ETFs, claridad regulatoria) y desde enero de 2026 los
procesadores que convierten cripto a fiat para comercios se clasifican como brokers
con obligaciones de reporte — señal de madurez, no de freno.

**Conclusión transversal:** la regulación llegó para formalizar, no para prohibir, y
quienes operan modelos **sin custodia con proveedores regulados** quedan mejor parados.

---

## 3. Nivel Latinoamérica: el laboratorio del mundo

LatAm recibió ~US$1,5 billones en valor cripto entre 2022 y 2025, con más del 90% en
stablecoins (Chainalysis). La región no adopta cripto por especulación sino por
**utilidad**: inflación, remesas y dolarización digital. Eso la convierte en el
laboratorio mundial del pago con cripto.

### 3.1 Brasil — el más avanzado (y el espejo de lo que viene)

- Mayor mercado cripto de la región (~US$318.800 millones recibidos, crecimiento
  >100% interanual — Chainalysis).
- **El patrón ganador: cripto sobre Pix** (el sistema de pagos instantáneos del banco
  central). Oobit integró USDT con Pix; Mercado Pago y Nubank ofrecen cripto; Lemon
  permitió a argentinos pagar por Pix en Brasil usando USDT "sin darse cuenta".
- **Dato clave para nosotros:** en Brasil, 26 millones de personas tienen cripto pero
  solo el 26% ha pagado alguna vez con ellas (Oobit/analistas). **La barrera no es la
  tenencia, es la usabilidad** — exactamente la tesis de nuestro proyecto.

### 3.2 Argentina — el más creativo por necesidad

- **Lemon:** la referencia regional. Tarjeta Visa que descuenta de saldo cripto +
  cashback en BTC + terminal de cobro QR + integración con Tienda Nube (15.000
  comercios online) + Transferencias 3.0 (QR interoperable nacional). Expandió a
  México, Perú, Colombia, Uruguay y Ecuador.
- **Ripio, Belo, Buenbit:** variantes del mismo patrón (wallet + tarjeta + pagos).
- **Lección argentina:** el usuario adopta cuando el pago con cripto se siente igual
  que el pago normal (mismo QR, misma tarjeta).

### 3.3 El movimiento regional más relevante para Colombia (2026)

**Bitget Wallet** anunció en mayo de 2026 pagos QR con stablecoins sobre los rieles
nacionales de Argentina (Transferencias 3.0), Bolivia (QR Simple) y **Colombia
(Bre-B)** — extendiendo lo que ya hacía en Brasil sobre Pix (Mobile Money LatAm).
Esto confirma dos cosas: (a) el modelo "stablecoin + riel nacional" viene hacia
Colombia con jugadores internacionales; (b) quedan preguntas regulatorias abiertas,
o sea que **nadie tiene aún la posición ganada**.

### 3.4 Stablecoins de moneda local — la infraestructura emergente

La región está creando stablecoins de monedas locales para eliminar la doble
conversión vía USD: MXNB (México, Bitso/Juno), BRL1 (Brasil), y en Colombia **COPM
(Minteo)** y **COPW (Wenia/Bancolombia)**. Plataformas B2B como VelaFi ya las enrutan
para pagos empresariales. Para nuestro proyecto son **rieles potenciales, no
competidores**.

---

## 4. Nivel Colombia: el mapa local detallado

### 4.1 La aceptación hoy (los números de la calle)

- **Binance Pay** registró más de 500 locales aceptando cripto en Colombia (mayo
  2025, CriptoNoticias) — desde barberías y restaurantes hasta cadenas.
- **La DIAN** contabilizó 680 establecimientos que aceptan pagos con cripto (177 solo
  en Bogotá) — dato de 2024 (Portafolio).
- **BTCmaps** registra 100+ comercios aceptando específicamente Bitcoin, impulsados
  por iniciativas como Trokera y la fundación Satoshi Team, que trabajan en reducir
  las barreras técnicas de aceptación (CriptoNoticias, 2026).
- Vía **Bitrefill** (gift cards con cripto) los usuarios ya "pagan" indirectamente en
  Rappi, Éxito, McDonald's, Netflix, etc. — un sustituto imperfecto que demuestra la
  demanda de gastar cripto.

**Lectura:** cientos de comercios en un país con ~500.000+ establecimientos formales
= adopción del 0,1%. El mercado está **sembrado** (hay pioneros, hay demanda
demostrada) pero **vacío** (nadie ha escalado la aceptación).

### 4.2 Los jugadores colombianos y quién hace qué

| Jugador | Qué hace | Relación con nosotros |
|---|---|---|
| **Payválida** | Pasarela colombiana que ya ofrece botón/QR cripto con liquidación en COP al banco del comercio; acepta Binance Pay, MetaMask, Trust Wallet; integra WooCommerce/Shopify/VTEX | **EL COMPETIDOR MÁS DIRECTO** identificado. Fortaleza: integración e-commerce. Debilidad aparente: enfoque online, no la calle ni el comercio físico pequeño |
| **Wenia (Bancolombia)** | Exchange del grupo Bancolombia; stablecoin COPW 1:1 con el peso (prueba de reservas con Chainlink); compra vía Nequi; Wenia Card Mastercard | Compite por el pagador (ruta tarjeta), no por el comercio. Podría ser proveedor/aliado de conversión |
| **Minteo (COPM)** | Stablecoin del peso colombiano sobre Celo, 200.000+ holders, auditada mensualmente (BDO); API para emitir/redimir | Proveedor potencial de rampa, no competidor |
| **Trokera + Satoshi Team** | Onboarding de comercios a Bitcoin (comunidad/educación) | Validan la tesis; posibles aliados de comunidad. Enfoque BTC, sin producto de plataforma completo |
| **PWC (PayWithCrypto)** | Startup local: QR de cobro cripto + retiro a banco colombiano + tarjeta | Competidor emergente pequeño; propuesta similar pero aparentemente con custodia propia — modelo regulatorio más pesado |
| **Buda, Bitso, CryptoMarket, Vurelo** | Exchanges/plataformas de trading y remesas | Lado del pagador y conversión; proveedores potenciales |
| **Mural Pay, Koywe, Cobre, VelaFi** | Infraestructura B2B de stablecoins/pagos transfronterizos con presencia en Colombia | Proveedores de rampa candidatos (el "enchufe" de nuestro modelo) |
| **Oobit** (respaldada por Tether) | Tarjeta cripto tap&pay que paga al comercio en COP vía red Visa | Ruta A (tarjeta): compite por el pagador, no crea red de comercios |
| **Bitget Wallet** | Anunció pagos QR con stablecoins sobre Bre-B | **La amenaza internacional a vigilar:** mismo espacio conceptual, llegando por el lado de la wallet |
| **Bold, Wompi, PayU** | Pasarelas tradicionales líderes | Aún sin cripto (sin evidencia encontrada); competidores potenciales si lo añaden — o compradores naturales |

### 4.3 El factor Bre-B (crítico, ventana de tiempo)

Colombia lanzó **Bre-B**, su sistema de pagos instantáneos interoperables (el "Pix
colombiano"). Todo el aprendizaje regional dice que la convergencia **stablecoin +
riel nacional** es el punto final del juego — Brasil ya lo demostró. Que Bitget
Wallet ya haya anunciado integración con Bre-B confirma que **la carrera en Colombia
ya empezó**.

**Implicación de diseño:** nuestro puerto de proveedor debe contemplar, a mediano
plazo, la **liquidación al comercio vía Bre-B** (a través del proveedor de rampa) —
velocidad de liquidación como ventaja competitiva.

---

## 5. Síntesis: el hueco que nadie ocupa

| Capa del problema | ¿Resuelto? | ¿Por quién? |
|---|---|---|
| Infraestructura de conversión cripto↔COP | Sí | Exchanges, rampas, stablecoins locales |
| Riel de pago del usuario Binance | Sí | Binance Pay |
| Riel de pago wallet/on-chain | Sí | Stablecoins + rampas |
| Gasto vía tarjeta (lado pagador) | Sí | Wenia Card, Oobit, Lemon, Binance Card |
| Checkout cripto para e-commerce | Parcial | Payválida, pasarelas globales |
| **ÚLTIMA MILLA del comercio físico pequeño:** venta presencial, onboarding en minutos, soporte local, reportes contables, directorio, educación | **NO** | **Nadie a escala — NUESTRO ESPACIO** |

El estado del arte **confirma la tesis del proyecto**: todos los componentes existen
(por eso nuestro modelo es orquestar, no construir infraestructura), la demanda está
demostrada (millones de holders, cientos de comercios pioneros, sustitutos
imperfectos como Bitrefill), y el hueco es exactamente el que identificamos — la
distribución y experiencia local para el comercio pequeño, el mismo hueco que Bold y
Wompi explotaron con los datáfonos frente a los bancos.

---

## 6. Implicaciones estratégicas

### Se confirma

- **El modelo de orquestación sin custodia es el correcto:** hasta los gigantes
  (Stripe) liquidan en fiat vía terceros; el valor está en la experiencia, no en
  tocar el dinero.
- **Stablecoins > Bitcoin para pagos:** priorizar USDT/USDC (y evaluar COPM/COPW) en
  el riel on-chain post-MVP.
- **Binance Pay como primer riel:** es el riel con más usuarios en Colombia y ya
  tiene 500+ comercios — validación directa.
- **La educación como producto:** el dato brasileño (26M holders, 26% ha pagado)
  demuestra que el cuello de botella es usabilidad + hábito, no tenencia.

### Se ajusta o se agrega

- **Vigilar a Payválida de cerca** (el competidor más parecido): estudiar su pricing,
  su onboarding y su cobertura física real. Nuestro diferencial debe ser la calle y
  el comercio pequeño, donde una pasarela orientada a integraciones no compite bien.
- **Bre-B entra al roadmap (mediano plazo):** liquidación instantánea al comercio vía
  el proveedor será un argumento de venta y una defensa frente a wallets
  internacionales que lleguen por arriba.
- **Wenia y Minteo como aliados potenciales, no rivales:** explorar COPW/COPM como
  rieles de conversión puede dar ventaja regulatoria (jugadores locales, auditados) y
  narrativa ("pesos digitales colombianos").
- **La ruta tarjeta no es enemigo directo pero sí un sustituto** para el pagador:
  nuestro directorio + comisiones menores al comercio + relación local son la
  respuesta.
- **Ventana de tiempo: 12-24 meses.** La convergencia con rieles nacionales y la
  entrada de jugadores internacionales ya comenzó. La velocidad del piloto importa.

---

## 7. Para el pitch a inversionistas

> "El mercado global de pasarelas cripto crece al 21% anual y los gigantes ya
> entraron — pero solo al e-commerce de EE.UU. y Europa. En LatAm, Brasil demostró el
> modelo (stablecoins sobre el riel nacional de pagos) y en Colombia todos los
> ingredientes ya existen: 6 millones de usuarios, 500+ comercios pioneros con
> Binance Pay, stablecoins de peso colombiano y el nuevo sistema Bre-B. Lo único que
> no existe es quien lleve esto al comercio de barrio con venta presencial, soporte
> local y reportes para el contador. Esa última milla fue exactamente donde Bold y
> Wompi construyeron sus negocios frente a los bancos — y es donde nosotros estamos
> construyendo el nuestro, con una ventana de 12-24 meses."

---

## Fuentes principales

Chainalysis (Geography of Cryptocurrency / LatAm 2025) · Cámara Colombiana de
Comercio Electrónico / Vurelo · CriptoNoticias · Portafolio · The Coin Republic ·
Mobile Money LatAm · DiarioBitcoin · Forbes Argentina · El Cronista · Binance Blog ·
Bancolombia/Wenia (sitio oficial) · Mural Pay · Spark Research · FXC Intelligence ·
Cobo/StablecoinInsider · ChangeHero.

*Documento de trabajo: verificar cifras puntuales antes de usarlas en materiales
públicos.*