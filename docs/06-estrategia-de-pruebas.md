# 06 — Estrategia de Pruebas

> En este proyecto **la prueba va primero** (TDD). El código sin prueba no está
> terminado. Esta es una práctica central de la cultura Ceiba.

---

## TDD: el ciclo

```
   ┌─────────────┐
   │ 1. RED      │  Escribe una prueba que falla (define el comportamiento deseado).
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │ 2. GREEN    │  Escribe el mínimo código para que pase.
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │ 3. REFACTOR │  Limpia el código manteniendo las pruebas en verde.
   └─────────────┘
```

Empezar por la prueba **fuerza un buen diseño**: si algo es difícil de probar,
normalmente está mal diseñado (mucho acoplamiento, demasiadas responsabilidades).

---

## Pirámide de pruebas

```
                    ▲   Pocas, lentas, caras
            ┌───────────────┐
            │   E2E / API   │   Flujo completo por HTTP (MockMvc / RestAssured)
            ├───────────────┤
            │  INTEGRACIÓN  │   Adaptadores reales: JPA con Testcontainers, webhooks
            ├───────────────┤
            │               │
            │   UNITARIAS   │   Dominio y casos de uso, sin infraestructura
            │  (la mayoría) │
            └───────────────┘
                    ▼   Muchas, rápidas, baratas
```

La mayor parte de las pruebas son **unitarias del dominio y los casos de uso**, porque
la arquitectura hexagonal permite probarlos **sin levantar Spring ni base de datos**.

---

## Nivel 1 — Pruebas unitarias del dominio (la joya de la hexagonal)

El dominio es puro Java, así que se prueba directo, rapidísimo, sin mocks de framework.

```java
class OrdenDePagoTest {

    @Test
    void confirmarPago_cuandoEstaPendienteYNoExpirada_pasaAPagoDetectado() {
        OrdenDePago orden = unaOrdenPendiente();
        EventoPago evento = unEventoDePago();

        orden.confirmarPago(evento);

        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGO_DETECTADO);
    }

    @Test
    void confirmarPago_cuandoYaEstaLiquidada_lanzaExcepcion() {
        OrdenDePago orden = unaOrdenLiquidada();

        assertThatThrownBy(() -> orden.confirmarPago(unEventoDePago()))
            .isInstanceOf(OrdenNoPuedeConfirmarseException.class);
    }

    @Test
    void estaExpirada_cuandoPasoElTiempoLimite_devuelveTrue() {
        OrdenDePago orden = unaOrdenQueExpiraEn(minutos(15));
        Instant despues = orden.expiraEn().plusSeconds(1);

        assertThat(orden.estaExpirada(despues)).isTrue();
    }
}
```

**Cobertura obligatoria del dominio:** todas las transiciones válidas, todas las
inválidas (que deben lanzar excepción), y todas las reglas de negocio (expiración,
límites, cálculo de comisión).

## Nivel 2 — Pruebas de casos de uso (aplicación)

Se prueba la orquestación con **implementaciones falsas de los puertos** (mocks o
*fakes*). Sin Spring.

```java
class CrearOrdenServiceTest {

    ComercioRepositorio comercios = new ComercioRepositorioFake();
    OrdenDePagoRepositorio ordenes = new OrdenDePagoRepositorioFake();
    ProveedorDePagoPort proveedor = mock(ProveedorDePagoPort.class);
    CrearOrdenService service = new CrearOrdenService(comercios, ordenes, proveedor);

    @Test
    void crear_conComercioVerificado_creaOrdenPendienteYPideCobro() {
        // dado un comercio verificado y un proveedor que responde con un QR
        // cuando se crea la orden
        // entonces la orden queda PENDIENTE_PAGO y se pidió el cobro al proveedor
    }

    @Test
    void crear_conComercioNoVerificado_rechaza() { ... }

    @Test
    void crear_conMontoSobreElLimite_rechaza() { ... }
}
```

> Preferir **fakes** (implementaciones simples en memoria) sobre mocks para los
> repositorios: las pruebas quedan más legibles y menos frágiles.

## Nivel 3 — Pruebas de integración (adaptadores reales)

Aquí sí se levanta la tecnología real, pero acotada. **Testcontainers** arranca un
PostgreSQL real en Docker para probar los repositorios JPA.

```java
@DataJpaTest
@Testcontainers
class OrdenRepositorioJpaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void guardarYRecuperar_persisteYMapeaCorrectamente() {
        // guarda una OrdenDePago vía el adaptador JPA
        // la recupera por referencia
        // verifica que el mapeo dominio <-> JPA es correcto
    }
}
```

Esto valida el **mapper** entre entidad de dominio y entidad JPA, las migraciones de
Flyway y las consultas — con una base de datos real, no un H2 que miente.

## Nivel 4 — Pruebas de API / webhook (flujo por HTTP)

Con `MockMvc` o RestAssured, se prueba el controlador y, sobre todo, el **webhook**:

```java
@SpringBootTest
class WebhookControllerTest {

    @Test
    void webhook_conFirmaValida_confirmaLaOrden() { ... }

    @Test
    void webhook_conFirmaInvalida_rechaza() { ... }

    @Test
    void webhook_mismoEventoDosVeces_procesaUnaSolaVez() {
        // idempotencia: enviar el mismo webhook 2 veces
        // la orden se confirma UNA vez, la segunda es no-op
    }
}
```

> La prueba de **idempotencia del webhook** es de las más importantes del proyecto:
> protege contra el doble cobro.

---

## Verificación de arquitectura con ArchUnit

Las reglas de dependencia no se dejan a la disciplina humana: se **verifican como una
prueba** que falla si alguien las rompe.

```java
@AnalyzeClasses(packages = "com.pasarela")
class ArquitecturaTest {

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_spring =
        noClasses().that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_jpa =
        noClasses().that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule la_aplicacion_no_depende_de_infraestructura =
        noClasses().that().resideInAPackage("..aplicacion..")
            .should().dependOnClassesThat().resideInAPackage("..infraestructura..");
}
```

Si un día alguien (¡o el asistente!) mete una anotación de JPA en el dominio, esta
prueba falla y lo detiene. Es el guardián automático de la arquitectura.

---

## Datos de prueba: patrón Object Mother / Builders

Para no repetir la construcción de objetos en cada prueba, usar métodos fábrica
legibles (`unaOrdenPendiente()`, `unComercioVerificado()`) o builders de test. Hace
las pruebas cortas y expresivas.

---

## Cobertura y calidad

- **Cobertura alta en el dominio y los casos de uso** (es donde vive el valor y el
  riesgo). No perseguir 100% ciego en infraestructura trivial.
- **SonarQube/SonarCloud** en el pipeline: sin *code smells* críticos ni vulnerabilidades.
- La cobertura es un **indicador, no un fin**: una prueba que no verifica
  comportamiento real no sirve aunque suba el número.

---

## Comandos

```bash
./mvnw test              # solo unitarias (rápido)
./mvnw verify            # unitarias + integración + ArchUnit + cobertura
```

Ejecuta `verify` antes de cada commit importante. En CI (GitHub Actions), `verify`
corre en cada push.

---

## Definición: una funcionalidad está probada cuando...

- [ ] Las transiciones/reglas de dominio tienen prueba unitaria (feliz + tristes).
- [ ] El caso de uso tiene prueba con puertos falsos.
- [ ] Si toca persistencia, hay prueba de integración con Testcontainers.
- [ ] Si es webhook, hay prueba de firma inválida y de idempotencia.
- [ ] ArchUnit sigue en verde.
