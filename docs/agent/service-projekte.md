# Service-Projekte: Actuator, SBOM, README, Changelog

Konfiguration steht in `konfiguration.md`. REST, OpenAPI und Security stehen in `architektur-service.md`.

## Actuator / Management-Endpunkte
Abhängigkeit: `implementation libs.spring.boot.starter.actuator`

| Endpunkt | Zweck | Konfiguration |
|---|---|---|
| `/actuator/loggers` | Logger anzeigen und steuern | `management.endpoints.web.exposure.include: loggers` |
| `/actuator/health` | Servicestatus | `include: health`, `management.endpoint.health.show-details: always` |
| `/actuator/info` | Build- und Versionsinfos | `include: info` und in der `build.gradle`: `springBoot { buildInfo() }` |

- Health: Prüfe externe Abhängigkeiten wie die DB über **eigene `HealthIndicator`-Implementierungen**.
- Abhängigkeiten zu **eigenen Services** werden **nicht** überwacht.

Swagger-UI liegt unter `/swagger-ui/index.html`, die API-Doku unter `/v3/api-docs` und `/v3/api-docs.yaml`.

## SBOM
In die `build.gradle` kommt `alias(libs.plugins.buildSbomReport)`.
In Multi-Project-Builds wendest du das Plugin **nur einmal** an, idealerweise im Projekt, das von den anderen abhängt.

## README – Mindestinhalt
- Kurzbeschreibung der Komponente
- Abschnitt „Entwicklerinformationen“ mit Links zu Build (gradle-wrapper), Publizieren (maven-publish-repo) und Changelog
- Tabelle aller Konfigurationsparameter (bei kleinen Projekten, sonst `documentation/index.md`)
- Ggf. Lizenzhinweis, falls nicht die „full-use“-Whitelist verwendet wird (`whitelist_for_only_internal_use.json`)
- Ggf. Up-/Download aus der projektlokalen Generic Registry
- Ggf. Migrationspfade bei DB-Änderungen

## Changelog-Vorlage
```markdown
# Changelog `versand-service` ("QD09")

## Version 1.1.1
- jira#DANN-1223 Versandstatus wird bei Timeout korrekt zurückgesetzt
```
