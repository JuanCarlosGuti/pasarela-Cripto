# Plan Maestro del Proyecto
## Pasarela de Pagos Cripto → Pesos para Comercios en Colombia

> **Documento integral para inversionistas y socios potenciales.**
> Cubre las dimensiones comercial, financiera, legal, administrativa, de mercadeo,
> funcional y técnica del proyecto — con la parte técnica explicada en lenguaje
> no técnico. Versión 1.0, documento de trabajo.

---

# ÍNDICE

1. [El negocio en una página](#1-el-negocio-en-una-página)
2. [Dimensión funcional: qué hace el producto](#2-dimensión-funcional)
3. [Dimensión comercial](#3-dimensión-comercial)
4. [Dimensión de mercadeo](#4-dimensión-de-mercadeo)
5. [Dimensión financiera](#5-dimensión-financiera)
6. [Dimensión legal y de cumplimiento](#6-dimensión-legal-y-de-cumplimiento)
7. [Dimensión administrativa y organizacional](#7-dimensión-administrativa)
8. [Dimensión técnica (explicada para no técnicos)](#8-dimensión-técnica)
9. [Hoja de ruta integral](#9-hoja-de-ruta-integral)
10. [Preguntas frecuentes de inversionistas](#10-preguntas-frecuentes)

---

# 1. EL NEGOCIO EN UNA PÁGINA

**El problema:** cerca de 6 millones de colombianos tienen criptomonedas, pero casi
ningún comercio las acepta. Ese dinero no tiene dónde gastarse sin fricción.

**La solución:** una plataforma que permite a cualquier comercio cobrar en cripto
mostrando un QR y **recibir pesos colombianos en su cuenta**. El comercio nunca toca
criptomonedas, no asume volatilidad y no necesita saber nada del tema. Tan simple
como cobrar con Nequi.

**Cómo ganamos dinero:** comisión del ~2-2.5% por transacción al comercio (comparable
al costo de un datáfono). Nuestro margen neto: ~1-1.5% tras pagar al proveedor que
hace la conversión.

**La clave del modelo:** **nunca custodiamos dinero ni cripto de nadie.** Somos
software que conecta las piezas; la conversión y la entrega de pesos al comercio las
hace un proveedor financiero regulado. Esto hace el negocio liviano en capital,
mínimo en riesgo regulatorio y limpio tributariamente.

**El momento:** Colombia es el quinto mercado cripto de América Latina (US$44.200
millones movidos en un año según Chainalysis), la adopción crece, las stablecoins
dominan, y la infraestructura para convertir cripto a pesos ya existe. Falta quien la
haga **accesible al comercio de barrio**. Ese es el espacio que ocupamos — el mismo
patrón con el que Bold y Wompi ganaron con los datáfonos.

**El fundador:** arquitecto de software con experiencia en plataformas de pago
empresariales **y** administrador de empresas de la Universidad Nacional — construye
la tecnología, entiende el negocio y sabe educar al mercado (7+ años como instructor).

---

# 2. DIMENSIÓN FUNCIONAL
## Qué hace el producto, contado como una historia

### La experiencia del comercio (nuestro cliente)

María tiene una barbería en Chapinero. Se registró en la plataforma en 10 minutos
desde su celular: datos del negocio, su cuenta de ahorros, listo. Al día siguiente
quedó verificada.

Un cliente le pregunta: *"¿puedo pagar con cripto?"*. María abre la página de cobro
en su celular, digita **$40.000**, y aparece un **código QR**. El cliente lo escanea
desde su app de Binance, confirma, y en la pantalla de María aparece **"PAGADO ✓"**
en segundos. Los pesos llegan a su cuenta según el ciclo de liquidación, y en su
panel queda el registro de la venta con su comprobante — listo para mostrárselo a su
contador a fin de mes.

María nunca vio una criptomoneda. Solo vendió un corte más.

### La experiencia del pagador (nuestro usuario)

Andrés recibió parte de su salario freelance en USDT. Antes, para gastarlo, tenía
que venderlo por P2P, esperar la transferencia, y recién entonces comprar. Ahora abre
el **directorio** de la plataforma, ve qué comercios cerca aceptan cripto, va a la
barbería de María, escanea y paga en 20 segundos.

### Funcionalidades del producto (MVP)

| Módulo | Qué hace |
|---|---|
| **Registro y verificación de comercios** | Alta en minutos; verificación con aprobación de un administrador |
| **Cobro por QR** | El comercio digita el monto; el sistema genera el QR/link de pago con expiración |
| **Link de pago por WhatsApp** | El mismo cobro, enviado como link — clave para ventas por WhatsApp |
| **Confirmación en tiempo real** | "PAGADO ✓" en la pantalla del comercio en segundos |
| **Registro y conciliación** | Cada pago, cada liquidación y cada comisión quedan registrados y cuadrados |
| **Dashboard del comercio** | Ventas del día/mes, historial exportable a Excel, comprobantes |
| **Controles de cumplimiento** | Límites por transacción y por mes; registro de operaciones inusuales |
| **Directorio de comercios** *(post-piloto)* | Mapa público "dónde pagar con cripto" — el imán de demanda |

### Lo que el producto NO hace (por diseño)

- No guarda ni mueve dinero de terceros (lo hace el proveedor regulado).
- No especula ni cambia divisas por cuenta propia.
- No exige al comercio entender nada de cripto.

---

# 3. DIMENSIÓN COMERCIAL

### A quién le vendemos

**Cliente que paga: el comercio.** Segmento inicial: negocios urbanos pequeños y
medianos con clientela joven/digital — tiendas de tecnología, barberías, gastronomía
de zona, gimnasios, freelancers — en **una zona concentrada** de Bogotá o Medellín.

**Usuario que atrae: el pagador cripto.** No paga por usar la plataforma; es el imán
que hace que el comercio quiera estar.

### La propuesta de valor, en palabras del cliente

> *"Me instalaron esto en una tarde. Ahora los muchachos que pagan con Binance me
> compran a mí y no al de la otra cuadra. Me llegan pesos, me cuesta lo mismo que el
> datáfono, y el reporte se lo mando directo al contador."*

### El modelo de venta

1. **Venta directa presencial** en la fase inicial (el fundador es el primer
   vendedor): visita, demo en vivo, instalación el mismo día, primer mes gratis.
2. **Servicio puente de "instalación de cobro digital"** (montarle al comercio su
   pasarela tradicional + la nuestra por una tarifa única): genera ingresos desde
   antes del lanzamiento y construye la base de clientes.
3. **Referidos y prescriptores** (comercio trae comercio; contadores de barrio como
   aliados) al escalar.
4. **Densidad de zona antes que dispersión:** saturar un barrio crea el efecto red
   local; luego se replica el playbook en la siguiente zona.

### Estado de la validación

- Primeros comercios contactados **ya manifestaron interés** en implementar la
  plataforma; su principal petición — *"que sea fácil de implementar"* — se convirtió
  en requisito número uno del producto.
- En curso: encuesta estructurada a base ampliada, midiendo **compromiso** (quién
  deja sus datos para el piloto), no opiniones.

---

# 4. DIMENSIÓN DE MERCADEO

### El mercado en cifras (fuentes públicas)

- **~6 millones de usuarios cripto** en Colombia (Cámara Colombiana de Comercio
  Electrónico / Vurelo, 2026).
- **US$44.200 millones** en transacciones cripto en un año — **5° mercado de
  Latinoamérica** (Chainalysis).
- Colombia **#29 del mundo** en adopción, subiendo 7 posiciones en un año.
- **Stablecoins ~90% del volumen regional**; más de la mitad de las compras cripto de
  colombianos terminan en stablecoins ("dolarización digital") — el activo ideal para
  pagos, sin volatilidad.
- **US$13.098 millones en remesas** en 2025 (+10,6%), flujo creciente por rieles
  digitales.
- Turismo y nómadas digitales (Medellín especialmente): llegan con cripto, sin pesos.

### Estrategia de marca y comunicación

- **Al comercio nunca le hablamos de cripto en técnico.** Promesa: *"vende más, sin
  riesgo, sin aprender nada nuevo"*.
- **Al pagador:** *"tu cripto por fin sirve para comprar"*.
- **La educación es el marketing:** guías de 20 segundos dentro de la propia página de
  pago; contenido corto para comunidades; el sticker en la puerta del comercio como
  valla publicitaria.
- **El directorio público de comercios** es el activo central: gratuito, con efecto
  red (más comercios → más pagadores → más comercios), y argumento de venta para
  afiliar nuevos negocios.

### Canales

| Lado | Canales |
|---|---|
| Comercios | Venta presencial → referidos → contadores/asociaciones de zona |
| Pagadores | Directorio/mapa → comunidades cripto (Telegram/X) → contenido educativo → punto de venta |

---

# 5. DIMENSIÓN FINANCIERA

### Modelo de ingresos

```
INGRESO PRINCIPAL: comisión por transacción
  Comisión al comercio:          ~2.0 – 2.5% del monto
  (–) Costo proveedor conversión: ~1.0%
  (=) Margen bruto plataforma:    ~1.0 – 1.5%

INGRESOS SECUNDARIOS:
  • Tarifa única de instalación "cobro digital completo" (desde el día uno)
  • Futuro: suscripción premium del dashboard; posicionamiento en el directorio
```

### Unit economics (lógica, a calibrar con el piloto)

La variable reina es el **TPV** (volumen total procesado). Ejemplo ilustrativo de la
mecánica — no una proyección:

```
Un comercio que procese $5.000.000 COP/mes por la plataforma
  → genera $100.000 – 125.000 en comisión bruta
  → deja $50.000 – 75.000 de margen a la plataforma
Con N comercios activos, el ingreso escala linealmente con el TPV,
sin que los costos crezcan al mismo ritmo (software).
```

**El piloto existe precisamente para medir:** ticket promedio real, transacciones por
comercio/mes, tasa de activación y retención — los cuatro números que convierten esta
lógica en proyección seria.

### Estructura de costos (liviana por diseño)

| Rubro | Naturaleza |
|---|---|
| Desarrollo | Principal inversión inicial (hoy: tiempo del fundador + herramientas) |
| Infraestructura cloud | Mínima en MVP (un servidor + base de datos gestionada) |
| Comercial y soporte | Crece con la tracción; principal destino de inversión externa |
| Legal y contable | Puntual y por gatillos (ver dimensión legal) |
| **Lo que NO tenemos** | Tesorería, custodia, capital regulatorio, licencias financieras costosas |

### Punto de equilibrio (concepto)

Con margen del ~1-1.5% del TPV, el punto de equilibrio se define por: TPV mensual ×
margen ≥ costos fijos mensuales. Con costos fijos deliberadamente bajos en la etapa
inicial, el equilibrio operativo es alcanzable con una base modesta de comercios
activos de buen ticket — y los ingresos por instalación acortan el camino.

### Uso de fondos propuesto (estructura de prioridades)

1. **Comercial y adquisición de comercios: 40-50%**
2. **Producto y desarrollo: 25-30%**
3. **Legal y cumplimiento: 10-15%**
4. **Operación y soporte: 10-15%**

**Hitos que compra la inversión:** MVP en producción → piloto validado con métricas →
50-100 comercios activos → datos duros para siguiente ronda o rentabilidad temprana.

---

# 6. DIMENSIÓN LEGAL Y DE CUMPLIMIENTO

### La decisión que define todo: sin custodia

La plataforma **nunca recibe, guarda, convierte ni administra** dinero o cripto de
terceros. Es un **proveedor tecnológico** (software de orquestación), no un
intermediario financiero. Consecuencias:

- **Carga regulatoria mínima:** las obligaciones pesadas (custodia, KYC profundo,
  registro como proveedor de servicios de activos virtuales) recaen en el proveedor
  de conversión, que ya está regulado.
- **Limpieza tributaria:** la SAS opera en **Régimen Simple de Tributación**, que
  tributa sobre ingresos brutos. Como ningún dinero de terceros pasa por nuestras
  cuentas, el único ingreso es la comisión — no hay riesgo de tributar sobre plata
  ajena. (Nota: existe doctrina DIAN sobre "gestión de criptoactivos" y el RST; la
  posición de la empresa — proveedor de software, sin custodia — es defendible y está
  documentada, y se validará con contador/abogado antes de facturar.)

### El vehículo

- **SAS constituida** (domicilio Valledupar; opera a nivel nacional), en proceso de
  puesta al día (renovación de matrícula; ajuste de objeto social cuando se requiera
  para el KYB con proveedores).

### El contexto regulatorio (transparencia)

- El marco cripto colombiano **está en evolución**: la DIAN ya obliga a las
  plataformas de criptoactivos a reportar operaciones (Resolución 000240 de 2025) y
  hay proyectos de ley en trámite para regular el sector.
- **Nuestra lectura:** la regulación entrante golpea a quien custodia e intermedia;
  a un orquestador de software lo afecta marginalmente, y el diseño por adaptadores
  permite ajustarse rápido. La regulación clara además **legitima el mercado** y
  probablemente acelere la adopción por comercios — nos favorece.

### Cumplimiento propio desde el día uno (proporcional)

- Límites por transacción y por comercio/mes; bitácora de operaciones inusuales.
- Verificación básica de comercios (RUT, cámara de comercio, representante legal);
  el KYC del pagador lo hace el proveedor.
- Protección de datos (Ley 1581): política de tratamiento y autorizaciones.
- Contratos clave revisados por abogado fintech **antes de mover dinero real**
  (gatillo definido): contrato con comercios, términos del pagador, acuerdo con
  proveedor.

### Gatillos legales (gasto legal solo cuando toca)

| Gatillo | Acción legal |
|---|---|
| Pedir producción al proveedor (KYB) | Certificados al día, ajuste de objeto social si aplica, RUT |
| Primera transacción con dinero real | Consulta abogado fintech + contratos + política de datos |
| Primera factura de comisión | Facturación electrónica activa + definición RST vs. ordinario con números reales |

---

# 7. DIMENSIÓN ADMINISTRATIVA
## Organización, roles y gobierno

### Hoy: fundador único con documentación exhaustiva

El proyecto lo lidera un fundador con perfil dual (arquitecto de software +
administrador de empresas UN). El riesgo de "fundador único" se mitiga con la
práctica menos común y más valiosa: **todo está documentado** — visión, alcance,
arquitectura, decisiones (ADRs), modelo de dominio, plan comercial — de modo que
cualquier persona que se incorpore es productiva rápido y el conocimiento no vive
solo en una cabeza.

### Estructura por etapas

| Etapa | Equipo |
|---|---|
| **MVP + piloto** | Fundador (producto, desarrollo, venta piloto) + contador externo + abogado fintech puntual |
| **Densidad (50-100 comercios)** | + 1 perfil comercial (calle) + soporte tercerizado o de medio tiempo |
| **Segunda ciudad** | + comercial 2 + primer desarrollador adicional |

### Gobierno y relación con socios

- Decisiones de arquitectura y de negocio **registradas por escrito** (ADRs y este
  plan): trazabilidad total para socios e inversionistas.
- Métricas del negocio (TPV, comercios activos, retención, margen) reportables
  mensualmente desde el piloto — el dashboard interno se construye a la par del
  producto.
- Apertura explícita a socios que aporten **distribución comercial** o **capital con
  red de contactos en comercio/fintech**; el frente técnico está cubierto por el
  fundador.

### Operación diaria (MVP)

- Verificación de comercios: manual, por el administrador (control de calidad y
  cumplimiento en la etapa donde más importa).
- Soporte: WhatsApp directo, con tiempos de respuesta comprometidos a los comercios
  del piloto.
- Conciliación: automática en el producto + revisión periódica.

---

# 8. DIMENSIÓN TÉCNICA
## Explicada para no técnicos (y por qué es una ventaja del negocio)

### La analogía: un director de orquesta

Nuestra plataforma **no es un banco ni una casa de cambio**. Es un **director de
orquesta**: no toca ningún instrumento (no toca el dinero), pero coordina a los
músicos — el comercio, la app del pagador y el proveedor financiero — para que la
canción (el pago) suene perfecta y en segundos.

### Cómo funciona un pago, sin tecnicismos

1. El comercio digita el monto → nuestra plataforma le pide al proveedor "prepara un
   cobro por $40.000" → aparece el QR.
2. El pagador escanea y paga desde su app → el dinero cripto va **al proveedor
   regulado, nunca a nosotros**.
3. El proveedor nos avisa "pago recibido" (una notificación firmada digitalmente,
   imposible de falsificar) → verificamos, registramos y le mostramos "PAGADO ✓" al
   comercio.
4. El proveedor convierte a pesos y **los deposita directo al comercio**. Nosotros
   registramos la operación y nuestra comisión.

### Las tres garantías técnicas que protegen el negocio

1. **Nunca se cobra doble.** Si la notificación de pago llega repetida (pasa en
   internet), el sistema la reconoce y la ignora: procesar el mismo aviso mil veces
   da el mismo resultado que procesarlo una. En pagos, esto se llama *idempotencia*
   y es la diferencia entre un sistema serio y un problema legal.
2. **Nada se pierde.** Si una notificación no llega (también pasa), un proceso
   automático revisa periódicamente las órdenes pendientes y las pone al día
   consultando al proveedor. El sistema siempre converge al estado correcto.
3. **Todo queda registrado.** Cada pago, cada cambio de estado y cada liquidación
   quedan guardados con fecha y hora — auditable ante el comercio, el contador, el
   proveedor o una autoridad.

### Por qué la arquitectura elegida es una ventaja competitiva

- **"Enchufes" intercambiables de proveedores:** el sistema está diseñado para que
  cada proveedor de pago (Binance hoy, otros mañana) sea como un electrodoméstico
  conectado a un enchufe estándar. Cambiar o añadir un proveedor **no requiere
  reconstruir nada** — se conecta uno nuevo. Esto elimina el mayor riesgo del modelo
  (depender de un solo tercero) y permite crecer a nuevos rieles de pago rápidamente.
- **Un solo sistema, ordenado por dentro** (no microservicios prematuros): más barato
  de operar, más rápido de evolucionar en esta etapa, y preparado para dividirse si
  algún día la escala lo exige. Frugalidad técnica = frugalidad de capital.
- **Calidad verificada automáticamente:** las reglas de construcción del software se
  comprueban con pruebas automáticas en cada cambio (incluidas pruebas que vigilan la
  propia arquitectura). Menos errores en producción = menos costo de soporte y más
  confianza del comercio.
- **Seguridad de base:** credenciales fuera del código, notificaciones verificadas
  con firma digital, accesos por roles, y ningún dato sensible en lugares expuestos.

### El stack, en una línea (para el lector técnico)

Java 25 / Spring Boot 3, arquitectura hexagonal con DDD, PostgreSQL, frontend Angular,
pruebas automatizadas (unitarias, integración con contenedores, verificación de
arquitectura), CI/CD. Documentación técnica completa disponible en `docs/` para
due diligence.

---

# 9. HOJA DE RUTA INTEGRAL

| Fase | Técnico | Comercial | Legal/Admin |
|---|---|---|---|
| **Ahora** | Fundaciones + núcleo de dominio | Validación con comercios + encuesta | Matrícula al día |
| **Desarrollo MVP** | Órdenes, webhooks, dashboard (con proveedor simulado) | Instalaciones "cobro digital" (ingresos puente); comercios comprometidos | — |
| **Pre-piloto** | Integración real Binance Pay (sandbox) | Materiales punto de venta listos | KYB proveedor; consulta abogado; contratos |
| **Piloto** | Producción + observabilidad | 5-10 comercios, una zona, primer mes gratis | Facturación al cerrar el gratis |
| **Densidad** | Segundo riel (on-chain); mejoras por feedback | 50-100 comercios; directorio público; primer comercial | Reporte mensual a socios |
| **Expansión** | App/PWA pulida; redundancia de proveedores | Segunda ciudad con playbook probado | Estructura societaria según ronda |

**Criterio de avance entre fases:** métricas cumplidas, no fechas. Cada fase tiene
sus umbrales definidos (ver Plan Comercial y documento de Alcance del MVP).

---

# 10. PREGUNTAS FRECUENTES DE INVERSIONISTAS

**"¿Esto es legal en Colombia?"**
Tener y transar cripto es legal en Colombia; lo que está en construcción es la
regulación específica del sector. Nuestro modelo — software sin custodia, con la
conversión en manos de un proveedor regulado — está diseñado precisamente para operar
con la mínima exposición mientras el marco madura, y se valida con asesoría legal
antes de mover dinero real.

**"¿Qué pasa si Binance o el proveedor los bloquea?"**
La arquitectura de "enchufes" permite conectar proveedores alternativos sin
reconstruir la plataforma. Multi-proveedor es parte del diseño desde el día uno,
y el segundo riel está en el roadmap inmediato post-MVP.

**"¿Por qué no lo hace Binance directamente?"**
Binance ofrece la tubería (Binance Pay), no la experiencia local: no visita
barberías en Chapinero, no da soporte por WhatsApp en español, no genera reportes
para el contador colombiano ni construye el directorio local. Nuestro valor es la
última milla — exactamente donde los gigantes no llegan y donde Bold/Wompi
construyeron su negocio frente a los bancos.

**"¿Y si la adopción de pago con cripto no despega?"**
Es el riesgo central y lo tratamos con honestidad: por eso el piloto es barato,
medible y previo a cualquier inversión fuerte en escala. Además, el servicio de
instalación de cobro digital genera ingresos independientes del despegue cripto, y
la relación construida con comercios tiene valor para múltiples productos futuros.

**"¿Qué compra mi inversión?"**
Velocidad comercial (el cuello de botella es la calle, no el código), el piloto
validado con métricas duras, y la posición de primer jugador enfocado en el comercio
físico pequeño — con un fundador que construye, vende y educa a la vez.

**"¿Cuál es la salida (exit)?"**
Tres caminos plausibles: (a) crecimiento rentable por comisiones con expansión
regional; (b) adquisición por una pasarela tradicional que quiera entrar a cripto
con la red ya construida; (c) adquisición/alianza por un exchange o rampa que quiera
la última milla comercial. La red de comercios y la marca de confianza son el activo.

---

*Documentos complementarios: [Documento para Inversionistas](01-documento-inversionistas.md)
(pitch detallado con cifras y fuentes) · [Plan Comercial y de Mercadeo](02-plan-comercial-mercadeo.md)
(ejecución de calle, guiones, métricas) · Documentación técnica completa en `docs/`.*
