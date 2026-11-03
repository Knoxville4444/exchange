# AGENTS.md

Arbeitsanweisungen für KI-Coding-Agenten (opencode, Claude Code u. a.) in DIZ-/QD-Projekten.
Diese Datei ist die **einzige Quelle der Wahrheit**. `CLAUDE.md` verweist nur hierher.

> Version 0.8 – Entwurf. Offene Punkte sind mit `TODO (OP-xx)` markiert und in `OFFENE-PUNKTE.md` gesammelt.
> Für dich als Agent gilt: Ein `TODO` heißt, dass die Regel noch nicht feststeht. **Frag nach, statt die Lücke selbst zu füllen.**

---

## 1. Grundhaltung

- Du arbeitest im **Behördenumfeld**. Nachvollziehbarkeit, Sicherheit und Konventionen gehen vor Eleganz und Geschwindigkeit.
- **Im Zweifel fragen statt raten.** Das gilt besonders für Abhängigkeiten, Architektur, Konfiguration, Datenbank und alles, was über die konkrete Aufgabe hinausgeht.
- Mach **kleine, überprüfbare Schritte**. Ändere nur, was für die Aufgabe nötig ist. Kein Umbau „bei der Gelegenheit“.
- **Erst planen, dann umsetzen:** Bei Aufgaben mit mehr als 3 Schritten legst du zuerst einen Plan vor und wartest auf die Freigabe. In OpenCode nutzt du dafür den Plan-Agenten, in Claude Code den Plan Mode.
- **Features vorschlagen, nicht einbauen:** Querschnittsfunktionen wie Metriken (Micrometer), Tracing, Caching oder Scheduling sind eigenständige Features. Hältst du so etwas für sinnvoll, **schlägst du es in deiner Zusammenfassung vor**. Ungefragt setzt du es nicht um.
- Erfinde keine internen Bibliotheken, Klassen, Gradle-Tasks oder URLs. Was du nicht im Projekt findest, existiert für dich nicht. Frag nach.
- **Bestehender Projektcode ist das beste Vorbild.** Bevor du etwas Neues anlegst, schau, wie das Projekt es bereits löst.
- **Code vor Wiki:** Widerspricht ein Wiki-Beispiel dem aktuellen Code (Projekt oder Sample-Projekt), gilt der Code. Ausnahme sind bekannte Schwächen, die in den Detail-Dokumenten als „nicht übernehmen“ markiert sind.

## 2. Tech-Stack

| Bereich | Version / Werkzeug |
|---|---|
| Java | **25** |
| Build | **Gradle 9** mit DIZ-Gradle-Distribution, Version-Catalog „LibCatalog“ |
| Framework | **Spring Boot 4.1.x** (Spring Framework 7, Jakarta EE, Jackson 3) |
| Tests | JUnit 5 (Jupiter), Mockito, AssertJ, **ArchUnit** (über `utils-test-lib`) |
| Logging | Log4j2 (nicht Logback) |
| Datenbank | Oracle oder PostgreSQL, Migrationen mit **Flyway** |
| Security | OIDC Resource Server (JWT), Rollen-Mapping über `permissions` |
| Interne Libs | `utils-spring-lib` (Security-Basis, Fehlerbehandlung), `utils-test-lib`, `utils-architecture-verification-lib` (ArchUnit) |
| Optional | Lombok, MapStruct (wenn im Projekt vorhanden) |
| Plattform | GitLab und GitLab CI, Nexus, Linux |

**Wichtig:** Dein Trainingswissen und viele Wiki-Beispiele stammen aus Spring Boot 2/3.
Lies **`docs/agent/spring-boot-4.md`**, bevor du Spring-Code, Jackson-Code oder Tests schreibst.

## 3. Befehle

Gebaut wird mit der **angepassten DIZ-Gradle-Distribution** (enthält u. a. die nötigen Zertifikate).
Der Gradle-Wrapper ist **nicht** im Projekt eingecheckt. Er wird einmal lokal ausgecheckt (Projekt `gradle-wrapper`), und jedes Projekt bekommt einen **Link `gradle`** auf dieses Verzeichnis (steht in `.gitignore`).

- **Lege niemals einen eigenen Wrapper an** (`gradle wrapper`, `gradlew` committen, `gradle/wrapper/*` erzeugen).
- **Lade keine Gradle-Distribution herunter** und nutze kein anderes, systemweit installiertes Gradle.
- Fehlt der Link oder schlägt der Build wegen Zertifikaten bzw. Repository-Zugriff fehl, **brich ab und melde es**. Repariere nichts an der Build-Umgebung.

Aufgerufen wird **`gradlew`** (über den Link bzw. `PATH`), **nicht** `./gradlew` und nicht `gradle`.

```bash
gradlew build           # kompletter Build inkl. Tests (wie in der CI)
gradlew assemble        # nur kompilieren / bauen
gradlew test            # Unit-Tests (inkl. ArchUnit)
gradlew check           # alle Prüfungen
# TODO (OP-01): projektspezifische Tasks (java-checks, Lizenz-/Vulnerability-Check, OpenAPI-Generierung)
```

**Nie veröffentlichen:** `gradlew publish*` und alle anderen Tasks, die Artefakte in den Nexus hochladen, sind tabu. Das macht ausschließlich die CI.

### Nexus (nur lesend)

Du darfst im Nexus **nachschlagen**, z. B. welche Versionen einer internen Lib oder einer OpenAPI existieren. Schreibend greifst du nie zu.

| Repository | URL |
|---|---|
| Releases | `https://NEXUS_HOST/repository/RELEASE_REPO/` – TODO (OP-02) |
| Snapshots | `https://NEXUS_HOST/repository/SNAPSHOT_REPO/` – TODO (OP-02) |

- Ruf die URLs **genau in dieser Form** auf: `curl -fsS <URL>`. Nur so greift die Freigabe in `opencode.json` bzw. `.claude/settings.json`, ohne dass jedes Mal nachgefragt wird.
- Für Abhängigkeiten nutzt du **Releases**. Snapshots verwendest du nur, wenn die Aufgabe das ausdrücklich verlangt. In `dependencies.gradle` kommen keine festen `-SNAPSHOT`-Versionen (siehe `docs/agent/dependencies.md`).
- Credentials für den Nexus schreibst du nirgendwohin. Verlangt der Zugriff eine Anmeldung, brichst du ab und meldest das.

Nach jeder Code-Änderung: **`gradlew test` muss grün sein**, bevor du die Aufgabe als erledigt meldest.

## 4. Was du darfst – und was nicht

**Ohne Rückfrage erlaubt**
- Code und Tests lesen, ändern und neu anlegen, sofern es zur Aufgabe gehört
- Bauen und Tests ausführen
- Git lesend nutzen (`status`, `diff`, `log`)

**Nur nach ausdrücklicher Freigabe**
- `git commit` (vorher Diff und Commit-Message zeigen)
- Neue Abhängigkeiten oder Versionsänderungen in `dependencies.gradle` / `build.gradle`
- Änderungen an `application.yml`, `application-template.yml`, `ci.yml` oder anderer Konfiguration
- **Neue Flyway-Migrationen** und jede Änderung an bestehenden Migrationen
- Neue Rollen, Änderungen an `@Secured`-Berechtigungen, am Rollen-Mapping (`permissions`) oder an der Security-Whitelist
- Löschen oder Umbenennen von Dateien und Packages

**Niemals**
- `git push`, Force-Push, Rebase veröffentlichter Branches, Tags
- Artefakte veröffentlichen (`gradlew publish*`, Uploads in den Nexus)
- `.gitlab-ci.yml` oder sonstige CI/CD-Konfiguration ändern
- Die **Version** in `gradle.properties` ändern (das passiert manuell)
- ArchUnit-Tests ändern, deaktivieren (`@Disabled`) oder umgehen. **Der Code wird angepasst, nicht der Test.**
- Tests löschen oder abschwächen, damit sie grün werden
- Secrets, Tokens, Passwörter oder echte personenbezogene Daten in Code, Tests, Logs, Konfiguration oder Commits schreiben
- Generierten Code (`build/`) committen
- Einen Endpunkt ohne `@Secured` anlegen. Einzige Ausnahme sind explizit gewhitelistete, unauthentifizierte Endpunkte, und deren Whitelist-Eintrag braucht eine Freigabe.

## 5. Coding Guidelines (Kurzfassung)

### Benennung und Packages
- Das Package passt zur Gradle-Gruppe bzw. zum Projektnamen. **Lege Packages nur unterhalb dieser Gruppe an.**
- Projektname und -gruppe in `gradle.properties` passen zum GitLab-Projekt und zur GitLab-Gruppe.
- Lib-Projekte (`*-lib`) sind aufgeteilt in **`api`**, **`model`** und **`impl`**.
- Die Struktur von Service-Projekten beschreibt **`docs/agent/architektur-service.md`**. Sie gilt für **QD- und DIZ-Dienste** gleichermaßen. Nur die Wurzel unterscheidet sich (`de.dpma.qd.service.<name>` bzw. `de.dpma.diz.service.<name>`). Welche gilt, entnimmst du der Gradle-Gruppe des Projekts.
- **Service-Typ:** Neue Dienste baust du als **„einfachen Dienst“**, also mit der klassischen Spring-Boot-Schichtung Resource → Service → Persistenz in **einem** Projekt. Das Wiki beschreibt zusätzlich einen DDD-orientierten **„komplexen Dienst“** mit eigenem Domain-Core-Projekt. Dieser Ansatz wird **derzeit nicht verfolgt**:
  - Lagere die Domänenlogik **nicht** in ein eigenes Gradle-Projekt bzw. Modul aus.
  - Führe keine DDD-Bausteine wie Aggregates, Domain Events, Ports & Adapters oder Anti-Corruption-Layer ein, nur weil das Wiki sie beschreibt.
  - Folgt ein **bestehendes** Projekt bereits dem komplexen Muster, behältst du dessen Struktur bei. Einen Umbau schlägst du höchstens vor.
- Was der einfache Dienst aus DDD übernimmt: **Fachobjekt und JPA-Entity sind in der Regel getrennt.** Die Details und die erlaubten Ausnahmen stehen in `docs/agent/architektur-service.md`.
- Der **Service ruft Repository und EntityMapper direkt auf.** Eine Zwischenschicht (Adapter, DAO, „Store“, Port) legst du nicht an.
- **REST-API: Code-first und API-first sind beide erlaubt.** Code-first erzeugt die OpenAPI aus den Annotationen, API-first generiert die Server-Interfaces und DTOs aus einer OpenAPI-Datei im Projekt. Hat das Projekt schon einen Ansatz, folgst du ihm. Bei einem neuen Dienst fragst du nach. Die Regeln für beide Varianten stehen in `docs/agent/architektur-service.md`, Abschnitt „REST-API“.
- **Externe Dienste** bindest du nur über Bibliotheken an: SOAP über eine Wrapper-Lib, REST über einen generierten OpenAPI-Client. Eigenen Client-Code schreibst du nicht. Der Aufrufcode liegt in `impl.client`.
- Namen sind sprechend. Gute Namen ersetzen Kommentare.

### Code
- Clean-Code-Prinzipien, soweit sinnvoll
- **Methodenreihenfolge = Aufrufreihenfolge**
- Sichtbarkeit so restriktiv wie möglich
- Eine Methode ist **höchstens etwa eine Bildschirmseite** lang.
- Kommentare nur bei echter Komplexität. Sie erklären das *Warum*, nicht das *Was*.
- Beachte die Hinweise von SonarLint und der IntelliJ-Analyse.
- Nutze moderne Java-Features, wo sie den Code klarer machen: `record`, Pattern Matching, `switch`-Ausdrücke, Text Blocks, Sequenced Collections.
- Datum und Zeit nur mit `java.time` (`LocalDate`, `Instant` …), **nicht** mit `java.util.Date`.
- **Lombok** und **MapStruct** sind gern gesehen, aber nicht Pflicht. Nutzt das Projekt sie schon, verwendest du sie auch. Fehlen sie, führst du sie nicht ohne Freigabe ein, denn das wäre eine neue Abhängigkeit.
- Injektion per **Konstruktor**, keine Feld-Injection.

### Formatierung (IntelliJ-Schema „ITZBund“, siehe `.editorconfig`)
- Einrückung mit 4 Leerzeichen, **keine Tabs**, Continuation Indent 4
- **Maximal 160 Zeichen** pro Zeile
- **Keine Wildcard-Imports** (`import foo.*`)
- Import-Reihenfolge: statische Imports, dann `java.*`, `javax.*`, `org.*`, `com.*`, Rest, jeweils durch eine Leerzeile getrennt
- **Immer geschweifte Klammern** bei `if`, `for`, `while` und `do`, auch bei Einzeilern
- Beim Umbruch stehen binäre Operatoren und `? :` **am Anfang der neuen Zeile**.
- Höchstens 1 Leerzeile in Folge im Code, 1 Leerzeile nach dem Klassenkopf
- Kein Ausrichten mehrzeiliger Parameter an der öffnenden Klammer

### Javadoc
- **Pflicht:** alle `public`-Methoden in `api`-Packages
- **Empfohlen:** Javadoc auf Klassenebene für alle Klassen und Interfaces, die außerhalb der Komponente sichtbar sind

### Logging
Details stehen in **`docs/agent/logging.md`**. Die wichtigsten Regeln:
```java
private final Logger log = LogManager.getLogger(getClass());   // NICHT static
log.debug("Ergebnis {}", () -> berechne());                     // Lambda statt isDebugEnabled()
```
- Alternativ `@Log4j2` (Lombok), falls das Lombok-Plugin im Projekt aktiv ist
- Bevorzugt loggst du technische IDs. Aktenzeichen sind erlaubt. **Namen, Adressen**, sonstige personenbezogene Daten, Tokens und vollständige Request-Bodies loggst du **nie**.

### Tests
- Tests sind sinnvoll und plausibel, Abdeckung allein ist kein Ziel.
- Teile Code so auf, dass er testbar ist.
- Tests sind derzeit in der Regel **reine Unit-Tests, die lokal und ohne externe Infrastruktur laufen**. Das heißt: keine externe Testumgebung, keine Testcontainers, keine Netzwerkzugriffe.
- Datenbankzugriffe testest du **nicht** mit eingebetteten In-Memory-Datenbanken wie HSQLDB oder H2. Solche Tests sagen wenig über das Verhalten der echten DB aus. Prüfe stattdessen die Logik rund um den Zugriff und isoliere die Persistenz an der Schnittstelle, z. B. mit Mocks am Repository.
- Hältst du einen Integrationstest für sinnvoll, **schlag ihn vor, statt ihn anzulegen**.
- **DB-Tests sind vorerst ausgeklammert.** TODO (OP-09): Das Thema wird später geregelt.
- Erlaubt sind Spring-Slice-Tests **ohne externe Infrastruktur**, insbesondere `@WebMvcTest` für REST-Endpunkte.
- **Keine Tests für Rollen-Mappings oder Berechtigungen**, insbesondere **kein KeycloakMock** und keine selbstgebauten Tokens. Der Mehrwert ist gering, die Komplexität hoch.
- Testaufbau: eine Testklasse pro Resource bzw. Service, eine `@Nested`-Klasse pro Methode, sprechende Testnamen. Details stehen in `docs/agent/architektur-service.md`.
- **ArchUnit:** Jeder Service hat einen ArchUnit-Test (`utils-architecture-verification-lib`). Die Regeln dort sind verbindlich, **der Code passt sich an**.

### Lib-Projekte
- **Reine Java-POJOs ohne Framework-Abhängigkeit.** Kein Spring, kein DI-Container, keine Annotationen wie `@Component`.
- Geschäftslogik in Libs folgt Clean Architecture bzw. Ports & Adapters. Das gilt **nur für Lib-Projekte**, nicht für Services (siehe „Service-Typ“).
- Konfiguration kommt per Konstruktor herein. Bei mehreren Parametern wird dafür ein eigener `record` angelegt.
- Brauchst du Nebenläufigkeit, wird ein `ExecutorService` von außen übergeben. Threading entscheidet nur die App bzw. Webapp.

## 6. Abhängigkeiten

**Bevor du `dependencies.gradle` anfasst, lies `docs/agent/dependencies.md`.** Kurzfassung:
1. Ohne Absprache kommt **keine neue Bibliothek** dazu. Frag vorher.
2. Steht die Abhängigkeit im **LibCatalog**, nutzt du `libs.xyz`. Wie du den Katalog nachschlägst, steht in `dependencies.md`. Aliase rätst du nicht.
3. Verwaltet Spring Boot die Abhängigkeit, gibst du **keine Version** an.
4. Interne Libs bekommen einen **Range**, z. B. `[4.0,5.0)`. Interne **OpenAPI**-Abhängigkeiten bekommen eine **feste Version**.
5. Gradle-Plugins kommen aus dem LibCatalog (`alias(libs.plugins.xyz)`), nicht direkt.

## 7. Datenbank, Konfiguration, verteilte Abläufe (Kurzfassung)

- **Keine herstellerspezifischen DB-Features**, insbesondere nichts Oracle-Spezifisches. Die Vereinheitlichung auf PostgreSQL steht an.
- Schema-Änderungen laufen **ausschließlich über Flyway**. In einem Migrationsschritt werden DDL und DML **nie** gemischt. Details stehen in `docs/agent/datenbank.md`.
- Eigene Konfiguration definierst du per `@ConfigurationProperties` mit `@Validated` oder per `@Value`. Property-Namen sind in **kebab-case**. **Keine Profile** in der `application.yml`. Details stehen in `docs/agent/konfiguration.md`.
- Ändert ein Ablauf eigene Daten **und** ein externes System, liest du vorher `docs/agent/transaktionen.md` (Idempotenz, Statemachine, Outbox).

## 8. Codegenerierung und Binärdateien
- Generate landen unter **`build/`**, nie in `src/`, und werden nicht committet. Das gilt auch für die bei API-first generierten Server-Interfaces und DTOs.
- Generierten Code passt du **nie von Hand** an. Ist das Ergebnis falsch, änderst du die OpenAPI-Datei oder die Generator-Konfiguration.
- Generierungs-Tools gehören nicht in den Laufzeit-Classpath.
- Große Binärdateien (Modelle, Bilder) kommen in die GitLab Generic Registry, nicht ins Repo.

## 9. Git-Workflow

- Branch-Namen nach GitLab-Standard (aus dem Issue erzeugt)
- **Commit-Message-Format:**
  ```
  jira#<PROJEKT>-<NUMMER> <kurze Beschreibung, wie im Changelog>
  ```
  Beispiel: `jira#DANN-1223 Versandstatus wird bei Timeout korrekt zurückgesetzt`
- Rate den JIRA-Key nicht. Ist er unbekannt, frag nach.
- Halte Commits klein und thematisch. Pro Commit gibt es eine logische Änderung.

## 10. Changelog

- Pflege **`changelog.md`** bei jeder für Test oder Betrieb relevanten Änderung. **Schlag den Eintrag vor**, der Mensch bestätigt ihn.
- Jeder Eintrag nennt die **JIRA-Nummer**, **nicht** die GitLab-Issue-ID.
- Kleine technische Anpassungen dürfen ohne Referenz eingetragen werden.
- Migrationspfade bei DB-Änderungen werden im Changelog oder in der README dokumentiert.
- Die Versionsüberschrift im Changelog legt der Mensch fest. Die Version in `gradle.properties` änderst du nicht.

## 11. Definition of Done (für den Agenten)

Bevor du eine Aufgabe als erledigt meldest:
- [ ] `gradlew test` ist grün, ArchUnit eingeschlossen.
- [ ] Für neue oder geänderte Logik gibt es Tests.
- [ ] Javadoc für neue `public`-API in `api`-Packages ist geschrieben.
- [ ] Neue Endpunkte haben `@Secured` (oder einen freigegebenen Whitelist-Eintrag), die passende Rollen-Extension und dokumentierte Fehlercodes: bei Code-first in `@Operation`, bei API-first in der OpenAPI-Datei.
- [ ] Neue Konfigurationsparameter stehen in der `application-template.yml` und in der Parametertabelle der Doku.
- [ ] Es gibt keine neuen Abhängigkeiten ohne Freigabe.
- [ ] Ein Changelog-Eintrag ist vorgeschlagen, falls die Änderung relevant ist.
- [ ] Die Zusammenfassung sagt, was geändert wurde, was offen ist und welche Annahmen getroffen wurden.

## 12. Detail-Dokumente

Lies diese Dateien **bei Bedarf** mit deinem Lese-Werkzeug:

| Datei | Wann lesen |
|---|---|
| `docs/agent/spring-boot-4.md` | vor Spring-, Jackson- und Testcode |
| `docs/agent/architektur-service.md` | vor Arbeit an Services: Service-Typ, Packages, Fachobjekt/Entity, externe Dienste, REST-API (Code-first/API-first), DTOs, Fehlerbehandlung, Security, Tests |
| `docs/agent/dependencies.md` | vor jeder Änderung an Abhängigkeiten oder Plugins |
| `docs/agent/datenbank.md` | vor Entities, Queries, Flyway-Migrationen |
| `docs/agent/konfiguration.md` | vor Änderungen an `application*.yml`, `ci.yml` und Properties |
| `docs/agent/transaktionen.md` | vor Abläufen über mehrere Systeme hinweg (DB + externer Service, Messaging) |
| `docs/agent/logging.md` | vor Logging-Konfiguration oder Logging-Abhängigkeiten |
| `docs/agent/service-projekte.md` | vor Actuator, SBOM, README, Changelog-Vorlage |
