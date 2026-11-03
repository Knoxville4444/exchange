# Offene Punkte – Agent-Setup

Sammelstelle für alles, was im Agent-Setup noch nicht entschieden oder eingetragen ist.
Im Text stehen die Stellen als `TODO (OP-xx)` markiert.

> **Für Menschen, nicht für den Agenten.** Diese Datei kommt nicht mit in die Projekte.
> Ist ein Punkt erledigt, entfernst du das `TODO (OP-xx)` an der Fundstelle und verschiebst den Punkt nach „Erledigt“.

## Offen

| ID | Thema | Fundstelle | Was fehlt |
|---|---|---|---|
| OP-01 | Projektspezifische Gradle-Tasks | `AGENTS.md` §3 | Namen der Tasks für java-checks, Lizenz-Check, Vulnerability-Check und OpenAPI-Generierung. Danach in die Allow-Listen von `opencode.json` und `.claude/settings.json` übernehmen. |
| OP-02 | Nexus-URLs | `AGENTS.md` §3, `opencode.json`, `.claude/settings.json` | Konkrete URLs für das Release- und das Snapshot-Repository. Die Platzhalter `NEXUS_HOST`, `RELEASE_REPO` und `SNAPSHOT_REPO` müssen in **allen drei** Dateien ersetzt werden, sonst greifen die Freigaben nicht. Ist eine Anmeldung nötig? |
| OP-03 | ArchUnit-Test | `docs/agent/architektur-service.md` | Name der Testklasse und Aufruf der Regeln laut aktualisiertem Sample |
| OP-04 | Fehlerbehandlung | `docs/agent/architektur-service.md` | `ServiceExceptionHandler` und `RestErrorInfo` laut aktualisiertem Sample: Klassen, Packages, Fehlercodes |
| OP-05 | Package für externe Clients | `docs/agent/architektur-service.md` | Prüfen, wie die letzten Services Aufrufe externer Dienste ablegen. Vorläufig gilt `impl.client`. |
| OP-06 | Doku-Template | `docs/agent/konfiguration.md` | Struktur von `documentation/index.md` bzw. der Parametertabelle |
| OP-08 | Erfahrungen aus der Boot-4-Migration | `docs/agent/spring-boot-4.md` | weitere Stolperfallen, z. B. Hibernate-Version |
| OP-12 | Zugriff auf den LibCatalog | `docs/agent/dependencies.md` Schritt 1, ggf. `opencode.json` / `.claude/settings.json` | Klären, wie der Katalog eingebunden ist, z. B. `versionCatalogs { libs { from("…") } }` in `settings.gradle` oder im Init-Skript der DIZ-Distribution. Danach den Zugriff für den Agenten festlegen: (1) **bevorzugt** die `.toml` per `curl` aus dem Nexus-Release-Repo (Koordinaten bzw. URL eintragen), (2) alternativ aus dem lokalen Gradle-Cache (`~/.gradle/caches/modules-2/…`, braucht eine Lesefreigabe für `external_directory`), (3) den GitLab-Link nur als Quelle für Menschen. |
| OP-09 | DB-Tests | `AGENTS.md` §5, `docs/agent/datenbank.md` | Spätere Regelung, ob und wie gegen eine DB getestet wird |
| OP-13 | API-first: Plugin und Veröffentlichung | `docs/agent/architektur-service.md` (Projektaufbau, REST-API) | Alias des openapi-generator-Plugins im LibCatalog. Wie wird die OpenAPI-Datei bei API-first veröffentlicht (z. B. als `openapi@zip` in den Nexus wie bei Code-first)? Entfällt dann `<service>-open-api`? Name des Generierungs-Tasks auch in OP-01 aufnehmen. |
| OP-14 | Rollen-Extension bei API-first | `docs/agent/architektur-service.md` (REST-API, Security) | Wie heißt `EXTENSION_PROPERTY_NAME` bei Code-first? Dieselbe Extension (Name und Format) sollte bei API-first in der YAML stehen. Vorläufig gilt `x-rollen: [rolle]`. Prüft die ArchUnit-Lib den Abgleich `@Secured` ↔ Extension, oder wäre das sinnvoll? |

## Erledigt

| ID | Thema | Ergebnis |
|---|---|---|
| OP-07 | Datenschutz beim Logging | Technische IDs bevorzugt, Aktenzeichen erlaubt, Namen und Adressen nie (v0.7) |
| OP-08 (Teil) | HTTP-Clients, Observability | Kein eigener Client-Code, RestClient wird bewusst nicht als Option genannt. Metriken und Tracing schlägt der Agent vor, er baut sie nicht selbst ein (v0.7). |
| OP-10 | Plan-Regel | gilt jetzt für alle Agenten in `AGENTS.md` §1, `CLAUDE.md` verweist nur noch auf den Plan Mode (v0.7) |
| OP-11 | Gradle-Link / `external_directory` | Keine Änderung. Der Agent ruft `gradlew` nur auf und muss im Wrapper-Verzeichnis nichts lesen. `external_directory` bleibt auf `ask`. |
| – | `opencode.json`: falscher Schlüssel `shell` | korrigiert auf `bash` (v0.7). Vorher griff **keine** Shell-Regel, auch das Push-Verbot nicht. |
| – | Permission-Lücken | Rückfrage jetzt auch bei `application-template.yml` und Flyway-Migrationen, Secrets in OpenCode lesegesperrt, `publish` gesperrt (v0.7) |
| – | Service-Typ einfach/komplex | Nur noch der einfache Dienst, DDD-Core-Projekt wird nicht verfolgt, Entity und Fachobjekt sind in der Regel getrennt (v0.7) |
| – | Externe Dienste | nur über Wrapper-Lib (SOAP) bzw. generierten OpenAPI-Client (REST), Aufrufcode in `impl.client` (v0.7) |
| – | Code-first / API-first | Beide sind erlaubt, ohne Begründung. Das Projekt gibt den Ansatz vor, bei neuen Diensten wird gefragt. API-first: Interfaces und DTOs generiert, Resource implementiert das Interface (v0.8) |
| – | Mapping-Ort Entity ↔ Fachobjekt | Der Service ruft Repository und EntityMapper direkt auf, ohne Zwischenschicht (v0.8) |
| – | „Herstellerspezifisch“ bei der DB | präzisiert: SQL-Funktionen, PL/SQL, Storage-Klauseln und native Queries ja, Datentypen der Ziel-DB in der DDL nein. Beispiel für `_TIME` und weitere Suffixe (v0.8) |
