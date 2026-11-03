# Abhängigkeiten und Versionierung

## Grundregeln
- **Keine nicht abgesprochene Einführung neuer Bibliotheken.** Frag vorher und begründe, warum die vorhandenen Mittel nicht reichen.
- Fremd-Bibliotheken dürfen keine bekannten sicherheitskritischen Issues haben. Das prüft der Vulnerability-Check im Build.
- Interne Abhängigkeiten sind korrekt und aktuell.
- Halte dich an Semantic Versioning: https://semver.org/lang/de/

## Prüfreihenfolge beim Hinzufügen (in `dependencies.gradle`)
1. **Steht die Abhängigkeit im LibCatalog?** Dann nutzt du sie zwingend aus dem Katalog (`libs.xyz`), damit die Version zentral gepflegt wird.
   - Schau zuerst nach, welche `libs.*`-Aliase in den `dependencies.gradle` und `build.gradle` des Projekts schon verwendet werden.
   - Aus einem TOML-Alias wird im Build-Skript ein Accessor mit Punkten: `spring-boot-starter-web` → `libs.spring.boot.starter.web`, Plugins über `libs.plugins.<alias>`.
   - Zugriff auf den vollständigen Katalog: TODO (OP-12). Bis das geklärt ist, rätst du keine Aliase. Findest du die Abhängigkeit nicht im Projekt, fragst du nach.
2. **Verwaltet Spring Boot die Abhängigkeit?** Dann gibst du **keine Versionsnummer** an, sonst wird das Spring Dependency Management überschrieben.
   Prüfen kannst du das, indem du die Abhängigkeit ohne Version einträgst und dann ausführst:
   ```bash
   gradlew assemble
   gradlew generateMetadataFileForJavaComponentsPublication
   gradlew generatePomFileForJavaComponentsPublication
   ```
3. **Ist sie weder im Katalog noch in Spring Boot enthalten?** Dann trägst du sie mit vollständiger Version ein.
4. Prüfe immer, ob es schon eine passende Abhängigkeit im LibCatalog oder in anderen Projekten gibt.
5. Wird eine Abhängigkeit in mehr als 3 Projekten genutzt, gehört sie in den LibCatalog. Das läuft über ein eigenes Issue und die Systemarchitekten, **nicht über den Agenten**.

## Schreibweisen (verbindlich)
```gradle
// Explizite Version (externe Lib, nicht im Katalog, nicht in Spring Boot)
implementation 'io.camunda:zeebe-client-java:8.1.4'

// Range bei interner Lib
implementation 'de.dpma.serviceclient:qd01-lib:[4.0,5.0)'

// KEINE Range bei OpenAPI-Abhängigkeiten (auch Minor-Changes können den generierten Client brechen)
openApi 'de.dpma.diz.service:doppelanmeldung-service-openapi:3.0.0:openapi@zip'

// Lib-Katalog
implementation libs.jackson.core
```

## Gradle-Plugins
Interne Plugins (java-assemble, java-unit-tests, vulnerability-check, license-check, java-checks, springdoc-openapi-generate-publish …) bindest du **über den LibCatalog** ein:
```gradle
alias(libs.plugins.<name>)
```
Binde Plugins nicht direkt per ID und Version ein.

## Fremd-APIs
Passe einen Service nur dann an eine geänderte Fremd-API an, wenn er die Änderung tatsächlich braucht. Ansonsten aktualisierst du die Abhängigkeit zum nächsten sinnvollen Zeitpunkt.
