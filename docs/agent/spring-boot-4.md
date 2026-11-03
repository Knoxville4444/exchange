# Stolperfallen: Spring Boot 4 / Java 25

Dein Trainingswissen basiert überwiegend auf Spring Boot 2/3. In diesem Projekt gilt **Spring Boot 4.1.x** mit Spring Framework 7.
Im Zweifel ist der vorhandene Projektcode maßgeblich, nicht dein Gedächtnis.

## Jackson 3
| Alt (Jackson 2) | Neu (Jackson 3) |
|---|---|
| `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` |
| `ObjectMapper` (veränderlich) | `JsonMapper` (unveränderlich, `JsonMapper.builder()`) |
| `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| `JsonProcessingException` (checked) | Exceptions sind unchecked, kein `try/catch`-Zwang |
| `JavaTimeModule` registrieren | nicht mehr nötig, `java.time` wird standardmäßig unterstützt |

- Ausnahme: Die **Annotationen** (`com.fasterxml.jackson.annotation.*`, z. B. `@JsonProperty`) bleiben im alten Package.
- Datumswerte werden standardmäßig als ISO-8601-String serialisiert.

## Tests
- `@MockBean` / `@SpyBean` → **`@MockitoBean` / `@MockitoSpyBean`** (`org.springframework.test.context.bean.override.mockito`)
- `@WebMvcTest`, `@AutoConfigureMockMvc` → Package `org.springframework.boot.webmvc.test.autoconfigure`
- `ErrorMvcAutoConfiguration` → `org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration`
- `EndpointRequest` (Actuator-Security) → `org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest`
- Test-Starter sind modularisiert, z. B. `spring-boot-starter-webmvc-test`.

## Starter und Auto-Configuration
- Der Starter heißt in Boot 4 `spring-boot-starter-webmvc`. Im LibCatalog bleibt der Alias `libs.spring.boot.starter.web`. Nutze immer den Alias.
- Auto-Configuration ist modularisiert. Eine reine Bibliothek (z. B. `flyway-core`) aktiviert **nicht** mehr automatisch die Auto-Configuration. Dafür brauchst du den passenden Starter.
- Verwende **nicht** `spring-boot-autoconfigure-classic`. Es ist nur eine Migrationskrücke.

## Null-Safety
- JSpecify statt Spring-Annotationen: `org.jspecify.annotations.NullMarked` und `org.jspecify.annotations.Nullable`.
- `org.springframework.lang.NonNullApi` und `org.springframework.lang.Nullable` sind deprecated.

## Allgemein
- Nur `jakarta.*`, **niemals** `javax.*` (außer JDK-eigene Packages wie `javax.crypto`).
- Java 25: `record`, `sealed`, Pattern Matching in `switch`, Record Patterns und Sequenced Collections sind erwünscht.
- Keine Preview-Features ohne Absprache.

## Wiki-Beispiele übersetzen
Viele interne Wiki-Beispiele haben den Stand Spring Boot 3.2 / Java 17. Übernimm sie **nicht wörtlich**:

| Im Wiki | In diesem Projekt |
|---|---|
| `'org.springframework.boot:spring-boot-starter-web'` direkt | immer den **LibCatalog-Alias** nutzen (z. B. `libs.spring.boot.starter.web`), der Katalog löst den Boot-4-Starter auf |
| `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| `new ObjectMapper()`, `JsonProcessingException` | `JsonMapper`, unchecked Exceptions |
| `@MockBean`, `@MockBeans({...})` | `@MockitoBean` an Feldern |
| `java.util.Date` in DTOs | `java.time.*` |
| `@UpdateMapping` | gibt es nicht. Nimm `@PutMapping` oder `@PatchMapping`. |
| feste `-SNAPSHOT`-Versionen interner Libs | Range nach `dependencies.md` |
| Basis-Image `openjre21` | Java 25. Das passende Image erfragen, nicht raten. |

## HTTP-Clients und Observability
- Eigene HTTP-Clients sind im Produktionscode nicht vorgesehen. Externe Dienste bindest du über generierte Clients bzw. Wrapper-Libs an (siehe `architektur-service.md`, „Externe Dienste“).
- In Tests prüfst du eigene Endpunkte mit MockMvc im `@WebMvcTest`, nicht über einen echten HTTP-Client.
- Micrometer wird für gezielte Fachmetriken genutzt, z. B. die Anzahl offener Jobs. Neue Metriken oder Tracing sind ein eigenes Feature: **vorschlagen, nicht einbauen** (siehe `AGENTS.md`, Grundhaltung).

TODO (OP-08): Weitere projektinterne Erfahrungen aus der Migration ergänzen (z. B. Hibernate-Version).
