# Vorlagenverwaltung – Querschnittsdienst Dokumentenerstellung (QD10)

Service- und API-Schicht für die **Verwaltung von Formularen, Bausteinen und Merkblättern**
gemäß OpenAPI `dokumentenerstellung-api.yaml` (v0.3.x). Die eigentliche Dokumenterzeugung
(LibreOffice) ist **nicht** Teil dieses Codes. Sie holt sich die Vorlagen über die Nahtstelle
`VorlagenAufloesung` (siehe unten).

Zielplattform: Java 25, Spring Boot 4.1, Spring Data JPA (Hibernate 7), Flyway, Oracle.

## Struktur

```
de.dpma.qd.service.dokumentenerstellung.verwaltung
├── domain           reines Java: Wertobjekte, Aggregat Vorlage/Version, Regeln, Fehler
│   └── fehler       FachlicherFehler → NichtGefunden (404), Konflikt (409), UngueltigeAnfrage (400), NichtUnterstuetzterInhalt (415)
├── service          Anwendungsfälle (Transaktionsgrenze), Ansichten, Anträge
│   ├── port         Ports: VorlagenRepository, KategorieRepository, InhaltSpeicher, AktuellerBenutzer
│   ├── inhalt       ODT-/PDF-/XSD-Prüfung, Erkennen der Baustein-Bereiche (nur JDK)
│   └── aufloesung   ★ Nahtstelle zur Erzeugung: VorlagenAufloesung
├── persistence      JPA-Entities, Spring-Data-Repositories, Adapter, Inhalte als BLOB
├── api              Skizze: Controller, DTOs, Fehlerbehandlung, Benutzer aus JWT
└── konfiguration    Clock-Bean
```

Die Trennung erfolgt per Paket, `ArchitekturTest` (ArchUnit) prüft sie:
`api → service ← persistence`, `domain` hängt von nichts ab, `service` kennt weder JPA noch Web.

## Fachliche Regeln im Code

| Regel | Ort |
|---|---|
| Versionsnummern vergibt der Service (HAUPT → `n.0`, NEBEN → `n.m+1`, auch für ältere Hauptversionen) | `Vorlage.planeNeueVersion` |
| Neue Version ohne Upload = Kopie von Inhalt, Schema und Bausteinen der Basisversion | `VersionService.legeAn` |
| Nebenversionen übernehmen das Schema der Hauptversion; Schema nur änderbar, solange die Hauptversion nie freigegeben war | `Vorlage.ersetzeSchema` |
| Effektiver Status wird aus Status und Zeitraum **berechnet**, es gibt keinen Scheduler | `Version.effektiverStatus` |
| Veröffentlichen, planen, verschieben, absagen, zurückziehen; `ab` nie in der Vergangenheit | `Vorlage.setzeVeroeffentlichung` |
| Vorlagen-Zeitraum: Änderungen wirken nur in die Zukunft (Vergangenheit bleibt rekonstruierbar) | `Zeitraum.gleicheVergangenheit` |
| Auflösung: höchste veröffentlichte passende Version zum Stichtag, Rückfall bei Rückzug | `Vorlage.loese` |
| Bausteine: Bereich `baustein:KAT/Name` im ODT (auch in Kopf-/Fußzeilen); neue Referenz → höchste Hauptversion | `InhaltPruefung`, `BausteinPruefung.gleicheAb` |
| Formular nur veröffentlichbar, wenn alle Bausteine im gesamten Nutzungszeitraum auflösbar sind | `BausteinPruefung.pruefeFormularVersion` |
| Baustein-Rückzug, -Absage oder -Zeitraum wird abgelehnt, wenn ein geplantes oder veröffentlichtes Formular ihn danach nicht mehr auflösen kann | `BausteinPruefung.pruefeBausteinAenderung` |
| Löschen nur für nie freigegebene Vorlagen bzw. Versionen; Bausteine nur ohne Referenzen | `VorlagenService`, `VersionService` |

## Nahtstelle zur Erzeugung

```java
@Autowired VorlagenAufloesung aufloesung;

AufgeloesteVorlage v = aufloesung.loese("PATGBM", "Bescheid",
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
und befüllt **danach** die Variablen. Für die Vorschau (auch für Entwürfe) gibt es
`loeseFuerVorschau(kategorie, name, version, bausteinEntwuerfe, stichtag)`.

## Einbindung ins Projekt

Benötigte Abhängigkeiten (vermutlich größtenteils vorhanden; die Starter-Namen für Boot 4 bitte gegen das Projekt prüfen):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-flyway'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.flywaydb:flyway-database-oracle'
runtimeOnly    'com.oracle.database.jdbc:ojdbc11'
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'com.tngtech.archunit:archunit-junit5'
```

Konfiguration (Auszug):

```yaml
spring:
  jpa:
    hibernate.ddl-auto: validate
    open-in-view: false
    properties:
      hibernate.default_batch_fetch_size: 50   # vermeidet N+1 beim Laden von Versionen/Bausteinen
      hibernate.jdbc.time_zone: UTC
  servlet.multipart:
    max-file-size: 20MB
    max-request-size: 25MB
```

Weitere Voraussetzungen:
- **Method Security** muss aktiv sein (`@EnableMethodSecurity`), damit die `@PreAuthorize`-Prüfungen der Controller greifen.
- **Scan:** Liegt die `@SpringBootApplication` in `de.dpma.qd.service.dokumentenerstellung` oder darüber, werden Entities, Repositories und Komponenten automatisch gefunden.
- **Flyway:** Die Migrationsnummer `V1__…` an die bestehenden Migrationen anpassen.

## Stand der Verifikation

- **Domäne und Service:** kompiliert, **67 Unit-Tests grün** (Domänenregeln, Baustein-Prüfungen, ODT/XSD-Prüfung, durchgängige Szenarien über alle Services mit In-Memory-Adaptern). Die Szenarien liegen in `src/test/java/.../service/VerwaltungSzenarioTest.java`.
- **Persistenz und API:** nur gegen nachgebaute API-Signaturen kompiliert (die Entwicklungsumgebung hatte keinen Zugriff auf Maven Central). **Beim ersten Bauen im Projekt prüfen**, vor allem die Spring-Boot-4-Starter, `Specification`, Multipart-Bindung und die Hibernate-Abbildung `Instant` → `TIMESTAMP WITH TIME ZONE`.
- **`ArchitekturTest`** ist geschrieben, wurde aber mangels ArchUnit nicht ausgeführt.

## Bewusst offen

- Mandantenfähigkeit bzw. Autorisierung je Fachsystem: Aktuell gibt es nur `SCOPE_formular-verwaltung` und `SCOPE_formular-public`.
- Fachsystemübergreifende Bausteine: technisch bereits möglich, da Referenzen `KATEGORIE/Name` enthalten. Die fachliche Regel ist noch offen.
- API-Skizze: Die Controller sind handgeschrieben. In der Umsetzung implementieren sie die generierten Interfaces, und die `Dtos` entfallen.
- Optimistisches Locking über HTTP (`ETag`/`If-Match`): Die `revision` liegt bereits im Aggregat.
- Aufräumen unreferenzierter Inhalte (z. B. nach gelöschten Entwürfen) als Betriebsjob.
- Vorschau-Endpunkt: gehört zur Erzeugung und nutzt `loeseFuerVorschau`.
