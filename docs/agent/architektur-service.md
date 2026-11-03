# Architektur-Blaupause „Einfacher Dienst“

Gilt für Service-Projekte, und zwar für **QD- und DIZ-Dienste** gleichermaßen. Ob ein Dienst QD oder DIZ ist, wird je Dienst entschieden. Die Regeln sind identisch, nur die Wurzel `<bereich>` (`qd` oder `diz`) unterscheidet sich. Sie folgt aus der Gradle-Gruppe.

> **Grundsatz: Code vor Wiki.** Referenz ist das Sample-Projekt `sample-spring-rest-service` (Boot-4-Stand).
> Widerspricht ein Wiki-Beispiel dem Sample oder dem bestehenden Projektcode, gilt der Code.
> Ausnahme: die Abschnitte unten, die mit **„Nicht aus dem Sample übernehmen“** markiert sind.
> TODO (OP-03, OP-04): Mit dem aktualisierten Sample-Projekt abgleichen (ArchUnit-Test, Fehlerbehandlung).

## Einordnung: einfacher vs. komplexer Dienst

Das Wiki unterscheidet zwei Grobtypen:

| | Einfacher Dienst | Komplexer Dienst (DDD) |
|---|---|---|
| Aufbau | ein Service-Projekt, klassische Schichtung Resource → Service → Persistenz | streng regulierter Domain-Core in eigenem Projekt, API/DB/externe Systeme in Nachbarprojekten |
| Status | **Standard für alle Dienste** | **derzeit nicht verfolgt** |

**Warum:** Durch den Serviceschnitt ist die fachliche Komplexität je Dienst bewusst gering. In bisherigen Diensten war der DDD-Aufbau umständlich und hat den erhofften Nutzen kaum gebracht.

Für dich heißt das:
- Wiki-Seiten und -Beispiele zum komplexen Dienst nimmst du **nicht** als Vorlage für neue Dienste. Ausnahmen sind die übernommenen Ansätze unten.
- **Kein eigenes Domain-Projekt** und kein eigenes Gradle-Modul für die Fachlogik. Die Fachlogik liegt in `impl` im Service-Projekt.
- Keine DDD-Bausteine wie Aggregates, Domain Events, Ports & Adapters oder Anti-Corruption-Layer „auf Vorrat“. Hältst du so etwas im Einzelfall für nötig, schlägst du es vor und begründest es.
- Ein **bestehendes** Projekt nach dem komplexen Muster behältst du, wie es ist. Einen Rückbau schlägst du höchstens vor, du baust nicht eigenmächtig um.

**Übernommen** wird aus dem DDD-Ansatz:
- die **Trennung von Fachobjekt und Entity** (siehe unten)
- die **Trennung von Fachobjekt und DTO**. Sie bleibt streng (siehe „Package-Struktur“).
- Fachlogik gehört in Services bzw. Fachobjekte, nicht in Resources, Entities oder Mapper.

## Projektaufbau (Gradle Multi-Project)

```
<projekt>/
├── build.gradle                      nur: alias(libs.plugins.buildJavaTest)
├── settings.gradle                   include('<service>'), include('<service>-open-api')
├── gradle.properties                 GroupName, ProjectName, ProjectVersion, usedLicenseWhiteListFile
├── README.md, changelog.md
├── .gitlab-ci.yml                    nur include des zentralen CI-Templates – nicht anfassen
├── .gitlab/merge_request_templates/  Default.md, Release.md
├── <service>/
│   ├── build.gradle                  Plugins (Reihenfolge beachten!), springBoot { buildInfo() }
│   ├── dependencies.gradle           alle Abhängigkeiten des Service
│   ├── application-template.yml      Konfigurationsvorlage für den Betrieb
│   └── src/...
└── <service>-open-api/              nur bei Code-first
    ├── build.gradle                  alias(libs.plugins.buildSpringdocOpenApi), customBootRun → ":<service>"
    └── gradle.properties             Publish-Repositories
```
Bei **API-first** liegt die OpenAPI-Datei stattdessen unter `<service>/src/main/openapi/<dienstname>-api.yaml`. TODO (OP-13): Wie die Datei bei API-first veröffentlicht wird und ob `<service>-open-api` dann entfällt.

**Plugin-Reihenfolge in `<service>/build.gradle` ist relevant:** `springBoot` muss **vor** `buildJavaPublish` stehen, sonst wird das ausführbare Jar nicht veröffentlicht. Ändere die Reihenfolge nicht.
```gradle
plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.lombok)
    alias(libs.plugins.mockito)
    alias(libs.plugins.buildJavaAssemble)
    alias(libs.plugins.buildJavaChecks)
    alias(libs.plugins.buildJavaChecksDeployable)
    alias(libs.plugins.buildJavaPublish)
}
```

## Package-Struktur

```
de.dpma.<bereich>.service.<name>
├── <Name>Application                 @SpringBootApplication (Einstiegspunkt)
├── <Name>AppConfiguration            @Configuration, @ConfigurationPropertiesScan, Beans (z. B. Rollen-Mapping)
├── api
│   └── v<major>
│       ├── ApiConfiguration          Security-FilterChain (extends AbstractApiConfiguration)
│       ├── OpenApiConfiguration      @OpenAPIDefinition, @SecurityScheme
│       ├── ...Resource               @RestController
│       ├── InboundApiMapper          Mapping DTO/Parameter → Fachobjekt (optional, bei Bedarf)
│       ├── OutboundApiMapper         Mapping Fachobjekt → DTO (z. B. MapStruct)
│       └── model
│           └── ...Dto / Enums        Datenmodell der API (bei API-first generiert, unter build/)
└── impl
    ├── <Fachobjekt>                  fachliches Modell (kein DTO, keine Entity!)
    ├── <...>Service                  fachliche Logik
    ├── persistence
    │   ├── <...>Entity               JPA-Entity
    │   ├── <...>Repository           Spring-Data-Repository
    │   └── <...>EntityMapper         Mapping Entity ↔ Fachobjekt
    └── client                        TODO (OP-05): Package-Name mit den letzten Services abgleichen
        └── <...>Client               Aufruf externer Dienste über Fremd-Lib bzw. generierten Client, inkl. Mapping
```
- **Fachobjekte (`impl`) und DTOs (`api.v?.model`) sind immer getrennt.** Resources geben nie Fachobjekte oder Entities direkt heraus, sondern mappen über einen Mapper in `api.v?`.
- Die Schichtung **Resource → Service → Repository** bleibt bestehen. Resources greifen nie direkt auf Repositories zu.
- Halte dich an die Klassennamen und Packages des bestehenden Projekts. Bei neuen Projekten nimmst du die Muster oben. Die ArchUnit-Regeln haben Vorrang vor diesem Schema.

## Fachobjekt und Entity

**Regelfall: getrennt.** Das Fachobjekt bildet die Fachlichkeit ab. Die Entity bildet das DB-Schema ab, mit JPA-Annotationen, technischen IDs, Versionsfeldern und Lazy-Loading.
- Services arbeiten mit **Fachobjekten**. Die Entity verlässt die Persistenzschicht nicht.
- Fachobjekte sind bevorzugt `record`s bzw. unveränderlich und tragen **keine** JPA- oder Jackson-Annotationen.
- Das Mapping Entity ↔ Fachobjekt liegt in einem eigenen Mapper bei der Persistenz. Ist MapStruct im Projekt vorhanden, nutzt du es, sonst mappst du von Hand.
- Fachlogik steht im Service oder im Fachobjekt, **nicht** in der Entity.
- Der **Service ruft Repository und EntityMapper direkt auf**: Er lädt die Entity, mappt sie zum Fachobjekt, wendet die Fachlogik an und überträgt das Ergebnis über den Mapper zurück auf die Entity. Eine Zwischenschicht (Adapter, DAO, „Store“, Repository-Port im Fachpackage) legst du nicht an.
- Regeln, die mehrere Services brauchen, legst du als eigenen Service oder als statische Hilfsklasse in `impl` ab, nicht in einer Basisklasse.

**Ausnahme: keine starre Regel.** Bei einfachem CRUD darf der Service direkt mit der Entity arbeiten, wenn Entity und Fachobjekt **1:1 identisch** wären und keine Fachlogik dranhängt. Ein reiner Durchreich-Mapper bringt dann nur Boilerplate.
- Folge dabei dem, was das Projekt bereits macht.
- Weichst du bei neuem Code vom Regelfall ab, **nennst du das in deiner Zusammenfassung** mit kurzer Begründung.
- Die DTO-Trennung gilt auch in diesem Fall: **Eine Entity geht nie über die REST-API hinaus.**

## Externe Dienste

Externe Dienste werden **immer über Bibliotheken** angebunden. Eigenen HTTP- oder SOAP-Client-Code schreibst du nicht.

| Art | Anbindung |
|---|---|
| **Legacy-Dienst (SOAP)** | über eine **externe Wrapper-Lib**, die den SOAP-Client kapselt. Den Client erzeugst du nicht selbst und nimmst ihn auch nicht selbst in Betrieb. |
| **REST-Dienst** | über einen **generierten OpenAPI-Client**. Die OpenAPI-Abhängigkeit bekommt eine feste Version (siehe `dependencies.md`). Der generierte Code liegt unter `build/`, wird nicht committet und nicht von Hand angepasst. |

- Der Code im Projekt, der den externen Dienst aufruft, liegt in **`impl.client`**, eine Klasse `...Client` pro externem Dienst. TODO (OP-05): Package-Name und Aufbau mit den letzten Services abgleichen. Bis dahin gilt: Hat das Projekt schon eine Konvention, folgst du ihr.
- Der `...Client` mappt zwischen den Typen der Lib bzw. des generierten Clients und den eigenen Fachobjekten. **Fremdtypen verlassen `impl.client` nicht.** Services sehen nur Fachobjekte.
- Kein `RestTemplate`, `RestClient`, `WebClient` oder JAX-WS-Code von Hand, um einen Client nachzubauen. Fehlt eine Wrapper-Lib oder eine OpenAPI-Spezifikation, **fragst du nach**.
- Ändert ein Ablauf eigene Daten **und** einen externen Dienst, liest du `transaktionen.md`.
- In Unit-Tests mockst du den `...Client`, nicht die Fremd-Lib bzw. den generierten Client.

## Zentrale Klassen

**Applikation** (Boot-4-Import beachten)
```java
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class SampleApplication {

    private SampleApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
```

**App-Konfiguration mit Rollen-Mapping**
```java
@Configuration
@ConfigurationPropertiesScan
public class SampleAppConfiguration {

    @Bean
    AuthoritiesConverter authoritiesConverter(final Map<String, ? extends Collection<String>> permissions) {
        return new AuthoritiesConverter(reverseMappingFunction(permissions));
    }

    @Bean
    @ConfigurationProperties(prefix = "permissions")
    Map<String, Collection<String>> permissions() {
        return new HashMap<>();
    }
}
```

## REST-API

Erlaubt sind **Code-first** und **API-first**. Eine Begründung für die Wahl ist nicht nötig.
- Hat das Projekt schon einen Ansatz, folgst du ihm. **Mischen innerhalb eines Dienstes ist nicht erlaubt.**
- Bei einem neuen Dienst fragst du, welcher Ansatz gilt.

Für beide Varianten gilt:
- **Basispfad:** `/api/<dienstname>/v<major>`. Der Dienstname ist kurz und kleingeschrieben, die Version nur die Major-Nummer.
- Resource-Klassen heißen `...Resource` und tragen `@RestController`. Ab etwa 10 Endpunkten verteilst du sie thematisch auf mehrere Klassen und OpenAPI-Tags.
- Abhängigkeiten werden per **Konstruktor** injiziert, nicht per Feld-Injection.
- Jede Handler-Methode trägt `@Secured`. Die Rollen stehen zusätzlich in der OpenAPI und müssen mit `@Secured` übereinstimmen (siehe „Security“).
- Die Doku-Regeln unten (Summary, Beschreibung, Parameter, Fehlercodes) gelten für beide Varianten. Bei Code-first stehen sie in den Annotationen, bei API-first in der OpenAPI-Datei.

### Code-first
- Die OpenAPI wird aus den Annotationen erzeugt und über das Unterprojekt `<service>-open-api` veröffentlicht.
- Die Resource trägt zusätzlich `@RequestMapping`.

### API-first
- Die OpenAPI-Datei im Projekt ist **führend**. Änderst du die API, änderst du zuerst die Datei und generierst dann neu.
- Generiert werden mit dem openapi-generator (Generator `spring`) **nur Interfaces und DTOs**, keine Controller-Implementierung. Das Plugin kommt aus dem LibCatalog. TODO (OP-13): Alias des Plugins im LibCatalog.
- Pflicht-Optionen (getestet mit openapi-generator 7.27.0):
  ```gradle
  def openApiAusgabe = layout.buildDirectory.dir('generated/openapi')

  openApiGenerate {
      generatorName = 'spring'
      library = 'spring-boot'
      inputSpec = file('src/main/openapi/<dienstname>-api.yaml').path
      outputDir = openApiAusgabe.get().asFile.path
      apiPackage = 'de.dpma.<bereich>.service.<name>.api.v<major>'
      modelPackage = 'de.dpma.<bereich>.service.<name>.api.v<major>.model'
      modelNameSuffix = 'Dto'
      globalProperties = [apis: '', models: '', supportingFiles: 'false',
              apiDocs: 'false', modelDocs: 'false', apiTests: 'false', modelTests: 'false']
      configOptions = [
              useSpringBoot4: 'true', useJackson3: 'true', interfaceOnly: 'true', useTags: 'true',
              skipDefaultInterface: 'true', requestMappingMode: 'api_interface', useResponseEntity: 'true',
              useBeanValidation: 'true', openApiNullable: 'false', useRecords: 'true', hideGenerationTimestamp: 'true']
  }

  sourceSets.main.java.srcDir(openApiAusgabe.map { it.dir('src/main/java') })
  tasks.named('compileJava') { dependsOn tasks.named('openApiGenerate') }
  ```
- Pro **Tag** entsteht ein Interface `<Tag>Api`. Die zugehörige `<Tag>Resource` **implementiert** es, trägt `@RestController` und `@Secured` je Methode, aber **kein** eigenes `@RequestMapping` (das Mapping steht im Interface).
- Die Resource mappt über `InboundApiMapper`/`OutboundApiMapper` zwischen generierten DTOs und Fachobjekten. Generierte DTOs gelangen nie in `impl`.
- Die Rollen stehen je Operation als Extension in der Datei. TODO (OP-14): Name der Extension festlegen und mit Code-first abgleichen. Vorläufig gilt `x-rollen`:
  ```yaml
  x-rollen: [formular_verwaltung]
  ```
  Der Generator wertet die Extension nicht aus. Du hältst `@Secured` und `x-rollen` von Hand synchron.
- Bekannte Stolperfallen:
  - Ein Multipart-Feld als Array mit `uniqueItems: true` erzeugt ein `Set` ohne Import. Lass `uniqueItems` dort weg.
  - Der generierte Code nutzt `org.springframework.lang.Nullable` (in Spring 7 deprecated). Das nimmst du hin und passt es nicht an.

### Handler-Methoden (Doku-Regeln – gelten, auch wenn das Sample sie nicht vollständig umsetzt)
Die Regeln sind für Code-first formuliert. Bei API-first setzt du sie sinngemäß in der OpenAPI-Datei um (`summary`, `description`, `parameters`, `responses`).
- Jeder Endpunkt hat `@Operation(summary, description, responses …)`.
- **Parameter:** `@PathVariable`, `@RequestParam` oder `@RequestBody`. Optionale Parameter bekommen `required = false`. Path- und Query-Parameter tragen zusätzlich `@Parameter(name, description)`.
- **Rückgabe:** ein DTO-Record oder Binärdaten. **Keine primitiven Typen bzw. nackten Strings.**
- **Erfolgscodes:** `200` (GET/PUT/DELETE), `201` (POST hat angelegt), `202` (Anlage erfolgt später)
- **Fehler** gehen an den Aufrufer **immer als JSON (`RestErrorInfo`)**, nie als Text. Jeder Fehlercode wird in `@Operation(responses = …)` dokumentiert. TODO (OP-04): Fehlerbehandlung (`ServiceExceptionHandler`) laut aktualisiertem Sample

## Datenmodell (DTOs)

- Liegt in `...api.v?.model` und besteht **nur aus `record`s und `enum`s**, ohne Logik.
- Records enden auf **`Dto`**.
- Bei **API-first** sind die DTOs generiert. Die folgenden Regeln setzt du dann in der OpenAPI-Datei um: `description` an jedem Schema und Attribut, `required`, Constraints (`minLength`, `pattern` …). Annotationen ergänzt du nicht von Hand.

Bei **Code-first** gilt zusätzlich:
- Der Record trägt `@Schema(name, description)`. Jedes Attribut trägt `@Schema(description, requiredMode = REQUIRED | NOT_REQUIRED)`.
- Pflichtattribute bekommen **zusätzlich `@NotNull`**, denn `requiredMode` wirkt nur auf die Doku.
- Bean Validation kommt aus `jakarta.validation.constraints`, **jede Annotation mit sprechender `message`**.
- Konsistenzprüfungen über mehrere Attribute machst du im kompakten Konstruktor des Records.
- Weicht `@Schema(name)` vom Feldnamen ab, brauchst du zusätzlich `@JsonProperty` mit demselben Namen.

## Security (OIDC Resource Server, RBAC)

- Abhängigkeiten: `libs.spring.boot.starter.security`, `libs.spring.boot.starter.oauth2.resource.server`
- Der Dienst ist ein **OIDC Resource Server** und validiert JWTs. Die Issuer-URI kommt aus `spring.security.oauth2.resourceserver.jwt.issuer-uri` und wird **nie hartcodiert**.
- **Rollen-Mapping:** Die Rollen des Dienstes (kleingeschrieben, z. B. `reader`, `tech_admin`) werden in der Konfiguration unter `permissions` auf Gruppen bzw. Rollen aus dem Token gemappt. Das Mapping pflegt der Betrieb über die `application-template.yml`.
  ```yaml
  permissions:
    reader:
      - ldap_gruppe_a
    tech_admin:
      - ldap_gruppe_admin
  ```
- **`ApiConfiguration`:**
  ```java
  @Configuration
  @EnableWebSecurity
  @EnableMethodSecurity(prePostEnabled = false, securedEnabled = true)
  public class ApiConfiguration extends AbstractApiConfiguration {
      @Bean
      SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http, final AuthoritiesConverter authConverter) throws Exception {
          return apiSecurityFilterChain(http, authConverter, x -> x.withSecurityMatch(toAnyEndpoint(), "tech_admin"));
      }
  }
  ```
  Die Actuator-Endpunkte sind auf `tech_admin` beschränkt.
- **Jeder Endpunkt trägt eine `@Secured`-Annotation** mit den berechtigten Rollen.
- **Ausnahme:** **explizit gewhitelistete**, unauthentifizierte Endpunkte sind erlaubt. Einträge in die Whitelist nimmst du **nur nach Freigabe** vor.
- Dieselben Rollen stehen **zusätzlich** in der OpenAPI: bei Code-first als Extension der `@Operation`, bei API-first als `x-rollen` an der Operation. `@Secured` und Extension **müssen übereinstimmen**. Code-first:
  ```java
  extensions = { @Extension(properties = @ExtensionProperty(name = EXTENSION_PROPERTY_NAME, value = "reader")) }
  ```
- **`OpenApiConfiguration`** (Code-first): `@SecurityScheme(type = OPENIDCONNECT, openIdConnectUrl = "${spring.security.oauth2.resourceserver.jwt.issuer-uri}/.well-known/openid-configuration")`.
  Der **Name** in `@SecurityScheme(name = …)` und in `@SecurityRequirement(name = …)` **muss identisch sein**.

## Tests

- **Keine Tests, die Rollen-Mappings oder Berechtigungen prüfen.** Dazu gehören insbesondere Tests mit **KeycloakMock** oder selbstgebauten Tokens. Der Mehrwert ist gering, die Testkomplexität hoch. Bestehende Tests dieser Art löschst du nicht, erweiterst sie aber auch nicht.
- Fachliche Logik (`impl`) testest du mit **reinen Unit-Tests** (JUnit 5, Mockito, AssertJ / `WithAssertions`).
- Resources testest du inhaltlich: entweder als Unit-Test mit gemocktem Service oder mit `@WebMvcTest` und deaktivierten Filtern (`@AutoConfigureMockMvc(addFilters = false)`), wenn die JSON-Serialisierung relevant ist. Mocks in Spring-Tests werden mit `@MockitoBean` erzeugt.
- Testkonfiguration: `src/test/resources/application-test.yml`, aktiviert über `@ActiveProfiles("test")`.
- Aufbau: eine Testklasse pro Resource bzw. Service, pro Methode eine `@Nested`-Klasse, sprechende Testnamen.
- **ArchUnit:** Jeder Service hat einen ArchUnit-Test (`utils-architecture-verification-lib`). TODO (OP-03): Klasse und Aufruf laut aktualisiertem Sample. Schlägt er fehl, ist **der Produktionscode falsch**. Der Test wird nicht geändert oder deaktiviert.
- Swagger-Verfügbarkeitstests legst du **nicht** an.

## Nicht aus dem Sample übernehmen
Das Sample hat bekannte Schwächen. Kopiere sie nicht:
- Endpunkte ohne `@Secured` (`/greeting` ist nur eine Demo)
- DTOs ohne `description` und `message` sowie `@NotNull` an primitiven Typen (`int`)
- Tabs zur Einrückung
- Abweichende Namen in `@SecurityScheme` und `@SecurityRequirement`
- Test-Bibliotheken im `implementation`-Scope (richtig ist `testImplementation`)
- Veraltete Angaben in der README (z. B. „Java 17“)
