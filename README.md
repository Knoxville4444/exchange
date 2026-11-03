# Vorlagenverwaltung – Querschnittsdienst Dokumentenerstellung (QD10)

Service- und API-Schicht für die **Verwaltung von Formularen, Bausteinen und Merkblättern**
gemäß OpenAPI `src/main/openapi/dokumentenerstellung-api.yaml` (0.4.0-SNAPSHOT). Die eigentliche
Dokumenterzeugung (LibreOffice) gehört **nicht** dazu. Sie holt sich die Vorlagen über
`VorlagenAufloesungService` (siehe unten).

Zielplattform: Java 25, Spring Boot 4.1, Spring Data JPA (Hibernate 7), Flyway, Oracle.
Aufbau nach `AGENTS.md` als **einfacher Dienst**: Resource → Service → Repository, ohne Ports,
Adapter oder Aggregate.

## Struktur

```
de.dpma.qd.service.dokumentenerstellung
├── api.v1             Resources (implementieren die generierten Interfaces), Inbound-/OutboundApiMapper,
│                      VorlagenExceptionHandler, Rollen
│   └── model          generiert aus der OpenAPI (*Dto), liegt unter build/generated/openapi
└── impl               Services, Fachobjekte als Records, Regeln (BausteinPruefung, InhaltPruefung)
    └── persistence    Entities, Spring-Data-Repositories, EntityMapper, VorlagenSuche (Specification)
```

- Die Services rufen Repository und EntityMapper **direkt** auf. Eine Zwischenschicht gibt es nicht.
- Fachliche Fehler sind eine einzige Klasse `FachlicherFehler` mit `Art` (NICHT_GEFUNDEN → 404,
  KONFLIKT → 409, UNGUELTIG → 400, FORMAT_NICHT_UNTERSTUETZT → 415) und einer `errorId`.
- Eine eigene Architekturprüfung ist nicht enthalten. Die übernimmt die ArchUnit-Lib des Projekts (OP-03).

## API-first: Generierung der Server-Stubs

Die OpenAPI ist führend. `openapi-generator.gradle` (openapi-generator 7.27.0, Generator `spring`) erzeugt
daraus je Tag ein Interface (`KategorienApi`, `VorlagenApi`, `VersionenApi`, `VersionsinhalteApi`,
`VeroeffentlichungApi`, `AbrufApi`, `ErzeugungApi`) sowie die Modelle als Records mit Suffix `Dto`.
Wichtige Optionen: `interfaceOnly`, `useSpringBoot4`, `useJackson3`, `useTags`, `skipDefaultInterface`,
`requestMappingMode=api_interface`, `openApiNullable=false`.

Einbinden in `build.gradle`:

```groovy
plugins {
    alias(libs.plugins.openapiGenerator)   // TODO: Alias im LibCatalog prüfen
}
apply from: 'openapi-generator.gradle'
```

Der generierte Code landet unter `build/generated/openapi` und wird **nicht** eingecheckt. `compileJava`
hängt von `openApiGenerate` ab.

Rollen stehen je Operation als `x-rollen` in der OpenAPI und zusätzlich als `@Secured(Rollen.…)` an den
Resource-Methoden. Der Generator wertet `x-rollen` nicht aus. Beides muss also von Hand synchron bleiben.

| Rolle | Darf |
|---|---|
| `formular_nutzer` | Erzeugung, Konvertierung, Merkblatt abrufen, Version auflösen |
| `formular_verwaltung` | Kategorien, Vorlagen, Versionen, Inhalte, Veröffentlichung, Vorschau |

`ErzeugungApi` (Erzeugung, Konvertierung, Vorschau) implementiert der Kollege, der die Erzeugung baut.

## Fachliche Regeln im Code

| Regel | Ort |
|---|---|
| Versionsnummern vergibt der Service (HAUPT → `n.0`, NEBEN → `n.m+1`, auch für ältere Hauptversionen) | `VersionService.legeAn` |
| Neue Version ohne Upload = Kopie von Inhalt, Schema und Bausteinen der Basisversion | `VersionService.legeAn` |
| Nebenversionen übernehmen das Schema der Hauptversion. Es ist nur änderbar, solange keine Version der Hauptversion freigegeben war | `VersionService.ersetzeSchema` |
| Der effektive Status wird aus Status und Zeitraum **berechnet**. Einen Scheduler gibt es nicht | `Version.effektiverStatus` |
| Veröffentlichen, planen, verschieben, absagen und zurückziehen. `ab` liegt nie in der Vergangenheit | `VeroeffentlichungService` |
| Beim Vorlagen-Zeitraum wirken Änderungen nur in die Zukunft | `Zeitraum.gleicheVergangenheit` |
| Auflösung: höchste veröffentlichte passende Version zum Stichtag, Rückfall bei Rückzug | `Vorlage.loese` |
| Bausteine sind Bereiche `baustein:KAT/Name` im ODT, auch in Kopf- und Fußzeilen. Eine neue Referenz zeigt auf die höchste Hauptversion | `InhaltPruefung`, `BausteinPruefung.gleicheAb` |
| Ein Formular ist nur veröffentlichbar, wenn alle Bausteine im gesamten Nutzungszeitraum auflösbar sind | `BausteinPruefung.pruefeFormularVersion` |
| Rückzug, Absage oder Zeitraumänderung eines Bausteins wird abgelehnt, wenn ein Formular ihn danach nicht mehr auflösen kann | `BausteinPruefung.pruefeBausteinAenderung` |
| Löschen geht nur bei nie freigegebenen Vorlagen bzw. Versionen, Bausteine zusätzlich nur ohne Referenzen | `VorlagenService`, `VersionService` |
| Gleichzeitiges Veröffentlichen von Formular und Baustein wird über eine Sperre auf der Vorlage serialisiert | `VorlageRepository.findeZumAendern` |

## Nahtstelle zur Erzeugung

```java
AufgeloesteVorlage v = vorlagenAufloesungService.loese("PATGBM", "Bescheid",
        VersionsAngabe.parseOptional(versionParam).orElse(null),   // "2", "2.3" oder null
        stichtagParam,                                              // null = jetzt
        VorlagenTyp.FORMULAR);

v.inhalt();            // Formular-ODT
v.schema();            // XSD zur Validierung der Variablen
v.bausteine();         // je Baustein: bereichsname ("baustein:PATGBM/Briefkopf"), version, ODT
v.version();           // → Header X-Formular-Version
v.bausteinHeader();    // → Header X-Formular-Bausteine
```

Die Erzeugung setzt den Inhalt jedes Baustein-ODT in den gleichnamigen Bereich des Formulars ein
und befüllt **danach** die Variablen. Für die Vorschau, auch von Entwürfen, gibt es
`loeseFuerVorschau(kategorie, name, version, bausteinEntwuerfe, stichtag)`.

## Einbindung ins Projekt

Benötigte Abhängigkeiten, vermutlich größtenteils schon vorhanden (die Starter-Namen für Boot 4 bitte gegen das Projekt prüfen):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-flyway'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.flywaydb:flyway-database-oracle'
runtimeOnly    'com.oracle.database.jdbc:ojdbc11'
```

Konfiguration (Auszug):

```yaml
spring:
  jpa:
    hibernate.ddl-auto: validate
    open-in-view: false
    properties:
      hibernate.default_batch_fetch_size: 50
      hibernate.jdbc.time_zone: UTC
  servlet.multipart:
    max-file-size: 20MB
    max-request-size: 25MB
```

Weitere Voraussetzungen:
- **Method Security** mit `@EnableMethodSecurity(securedEnabled = true)`, sonst greifen die `@Secured`-Annotationen nicht.
- **Flyway:** `V1.0.0__Initialcreate.sql` enthält nur DDL und keine Oracle-Storage-Klauseln. Ob `ddl-auto=validate` damit durchläuft, muss ein Mensch gegen eine echte Oracle prüfen.

## Stand der Verifikation

- **Generierung:** Der Generator lief gegen die OpenAPI. Der Main-Code inklusive aller Resources kompiliert gegen die generierten Interfaces und Modelle, die Signaturen passen also.
- **Fachlogik und Services:** **86 Unit-Tests grün.** Das betrifft Fachobjekte, Baustein- und ODT/XSD-Prüfung sowie alle Services mit gemockten Repositories.
- **Nicht gegen echtes Spring/Hibernate gelaufen:** Die Entwicklungsumgebung hatte keinen Zugriff auf Maven Central. Kompiliert wurde gegen nachgebaute Signaturen. **Beim ersten Bauen im Projekt prüfen:** Boot-4-Starter, `Specification`, Multipart-Bindung (insbesondere `tags` als Liste), Hibernate-Abbildung `Instant` → `TIMESTAMP WITH TIME ZONE` und `ddl-auto=validate`.
- Der generierte Code nutzt `org.springframework.lang.Nullable`. In Spring 7 ist das deprecated (JSpecify), funktioniert aber.

## Bewusst offen

- Fehlerformat (`RestErrorInfoDto`) und `errorId`-Katalog: siehe TODO OP-04 im `VorlagenExceptionHandler`.
- Autorisierung je Fachsystem bzw. Kategorie: Aktuell gibt es nur die zwei Rollen.
- Fachsystemübergreifende Bausteine: technisch schon möglich, da Referenzen `KATEGORIE/Name` enthalten. Die fachliche Regel fehlt noch.
- Optimistisches Locking über HTTP (`ETag`/`If-Match`).
- Aufräumen unreferenzierter Inhalte als Betriebsjob, z. B. nach gelöschten Entwürfen.
- Generator-Bug: Ein Multipart-Array mit `uniqueItems` erzeugt ein `Set` ohne Import. Deshalb ist `tags` in `VorlageCreateRequest` eine Liste.
