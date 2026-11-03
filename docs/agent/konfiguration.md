# Spring-Boot-Konfiguration

## Eigene Parameter
- Du definierst sie im Code per **`@ConfigurationProperties`** (mit **`@Validated`** und Validierungsannotationen) oder per `@Value`. Defaults setzt du ebenfalls dort.
- Für zusammengehörige Parameter bevorzugst du `@ConfigurationProperties` als `record`.
- Namen und Prefixe sind in **lower-kebab-case**, z. B. `servicename.dms-url`, nicht `dmsUrl`.
- Eigene Parameter wiederholst du **nicht** in der internen `application.yml`, nur um sie zu defaulten.

## Interne `application.yml` (`src/main/resources`)
- Parameter externer Abhängigkeiten (Spring-Starter, `utils-spring-lib`) werden auf eigene Parameter gemappt und so vor dem Betrieb gekapselt:
  ```yaml
  spring:
    datasource:
      url: ${servicename.datasource.url}
    transaction:
      default-timeout: ${servicename.transaction-timeout}
  ```
- **Kein Profil festlegen.** Vermeide Profile grundsätzlich.
- **Kein `spring.config.import`** in der internen `application.yml`, sondern nur in der Template-Datei.
- Externe Ressourcen benennst du nach dem Muster `<name>-service.url`:
  ```yaml
  dms-service:
    url:
  ```

## Sensible Parameter
- Passwörter und Credentials werden **niemals versioniert**.
- Der Betrieb legt sie als **Secret Files** ab (Config Tree). Der Dateiname ist der kanonische Property-Name in kebab-case, z. B. `servicename.db.password`.
- Im Code bzw. in der `application.yml` werden sie wie normale Properties referenziert.

## `application-template.yml` (im Hauptverzeichnis des (Sub-)Projekts, versioniert)
Sie ist die Vorlage für Betrieb und lokale Ausführung und enthält **alle vom Betrieb anzupassenden, nicht sensiblen** Parameter:
- **Pflichtparameter ohne Default:** nicht auskommentiert, Wert leer, kurzer Kommentar zum Zweck
- **Optionale Parameter:** auskommentiert, mit Default als Wert
- **Sensible Parameter:** nur als Kommentar, z. B. `# DB-Passwort bitte als Secret File myapp.db.password`
- Der Config-Import für Secrets steht hier:
  ```yaml
  spring.config.import: # z. B. optional:configtree:/path/to/my/secrets
  ```
- Starre Parameter aus der internen `application.yml` wiederholst du hier **nicht**.

Zusätzlich gehören **alle** Parameter tabellarisch in die `README.md` (kleine Projekte) oder in `documentation/index.md`. TODO (OP-06): Struktur des Doku-Templates ergänzen

## Tests
Testkonfiguration steht in `src/test/resources/application-test.yml` und wird mit `@ActiveProfiles("test")` aktiviert (Stand Sample-Projekt, Code vor Wiki). Das ist die einzige erlaubte Ausnahme von „keine Profile“.

## Lokale Entwicklung
Lokale Konfiguration wie `application-dev.yml` oder IDE-Run-Configs wird **nicht versioniert**.

## `ci.yml` (Minimalausführung in der CI, z. B. OpenAPI-Generierung)
- Minimal, aber vollständig lauffähig, **ohne reale Hosts und Ports**
- Die `springdoc.*`-Parameter entsprechen der internen `application.yml`.
- Irrelevante Fehler- und Warnmeldungen unterdrückst du per Logging-Konfiguration:
  ```yaml
  logging:
    level:
      org.hibernate: OFF
      com.zaxxer: OFF
  ```
- Aufruf mit `--spring.config.name=ci.yml`

## Betrieb (zur Info)
Der Betrieb nutzt eine externe, zusätzliche `application.yml` auf Basis des Templates, eingebunden über `spring.config.additional-location`. Ziel ist eine möglichst sparsame betriebsseitige Zusatzkonfiguration. Soll ein neuer Parameter einen Default bekommen, wird das per Issue geklärt.
