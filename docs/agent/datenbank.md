# Datenbank und Flyway

## Grundsätze
- Oracle ist derzeit Standard für dauerhafte Persistenz. PostgreSQL läuft im DIZ für Daten mit begrenzter Relevanzdauer. **Eine Vereinheitlichung auf PostgreSQL zeichnet sich ab.**
- Verwende **keine herstellerspezifischen Features**, insbesondere nichts Oracle-Spezifisches. Bleib bei Standard-SQL bzw. JPA. Gemeint sind:
  - SQL-Funktionen und -Syntax wie `ROWNUM`, `CONNECT BY`, `NVL`, `DECODE`, `MERGE`, Optimizer-Hints
  - PL/SQL, Trigger, Stored Procedures, Packages
  - Storage-Klauseln wie `TABLESPACE`, `STORAGE`, `LOB (...) STORE AS`, `PCTFREE`, Partitionierung. Das regelt der Betrieb.
  - native Queries, solange JPQL bzw. Criteria/Specification reichen
- **Nicht** gemeint sind die Datentypen der Ziel-DB in der Flyway-DDL (z. B. `VARCHAR2`, `NUMBER`, `BLOB`, `TIMESTAMP WITH TIME ZONE` bei Oracle). Die sind in DDL unvermeidlich. Nimm dabei möglichst Typen, die es in PostgreSQL gleichwertig gibt.
- Welche DB ein Projekt nutzt, erkennst du am Treiber in `dependencies.gradle`. Ist das unklar, frag nach.
- Migrationen laufen **ausschließlich über Flyway**, in Spring-Boot-Anwendungen im Spring-Boot-Kontext.
- Entities sind in der Regel vom Fachobjekt getrennt und verlassen die Persistenzschicht nicht. Regel und Ausnahmen stehen in `architektur-service.md`, Abschnitt „Fachobjekt und Entity“.
- In Boot 4 brauchst du dafür den Starter (`spring-boot-starter-flyway` bzw. den Eintrag im LibCatalog). `flyway-core` allein reicht nicht.

## Namenskonventionen
- **Oracle:** `UPPER_SNAKE_CASE` – `[A-Z][A-Z0-9]*(_[A-Z0-9]+)*`
- **PostgreSQL:** `lower_snake_case` – `[a-z][a-z0-9]*(_[a-z0-9]+)*`
- **Kein explizites Quoting** in eigenen Skripten
- Im selben Namensraum dürfen sich keine zwei Namen nur durch Groß- und Kleinschreibung unterscheiden.
- Sequenzen haben das Präfix `SEQ_`.
- Suffixe: IDs `_ID`, Datum `_DATE`, Zeitstempel `_TIME`, Boolean `_FLAG`. Das gilt auch für fachliche Zeitpunkte:
  ```sql
  ERSTELLT_TIME           TIMESTAMP WITH TIME ZONE NOT NULL,
  VEROEFFENTLICHT_AB_TIME TIMESTAMP WITH TIME ZONE,
  GUELTIG_AB_DATE         DATE,
  AKTIV_FLAG              NUMBER(1) DEFAULT 0 NOT NULL
  ```
- Jede Spalte bekommt einen kurzen Kommentar:
  ```sql
  COMMENT ON COLUMN VERSAND_ADRESSE.PREFIX IS 'Namensvorsatz';
  ```
- Ein neues Flyway-Skript wird gegen die Entities mit `spring.jpa.hibernate.ddl-auto=validate` geprüft. Das macht der Mensch mit einer echten DB, nicht der Agent. **Weise in deiner Zusammenfassung ausdrücklich darauf hin**, wenn du ein Skript angelegt oder geändert hast.

> **DB-Tests sind vorerst bewusst ausgeklammert.** Schreibe keine automatisierten Tests gegen eine Datenbank, weder In-Memory noch echt. Diese Regel wird später ggf. erweitert.

## Migrationen
- **SQL-Skripte** liegen in `src/main/resources/db/migration` und folgen den Flyway-Namenskonventionen.
- Für die erste Migration nimmst du `V1.0.0__Initialcreate`. Solange es noch keinen Release-Kandidaten gibt, darf sie angepasst werden.
- **DDL bevorzugt als SQL, DML bevorzugt als Java-Migration.** Ausnahme: einfache Inserts in eine neue Tabelle.
- **DDL und DML werden nie in einem Migrationsschritt gemischt.** Die Transaktion steuert Flyway.
- **Update-Migrationen werden nie zusammen mit einem Major-Release ausgerollt.** Alte und neue Major-Version müssen eine Zeit lang parallel laufen können.
- Eine Neuinstallation auf leerer DB muss „clean sheet“ durchlaufen. Java-Migrationen dürfen auf leerer DB nicht scheitern.
- Migrationspfade werden dokumentiert (Changelog, README oder Doku-Verzeichnis).

### Dreischritt bei Update-Migrationen
1. **DDL ohne Restriktionen:** neue Tabellen, neue *nullable* Spalten. Die alte Service-Version muss danach noch laufen. Alte Constraints löschst du nur, wenn Schritt 2 das erfordert.
2. **DML:** Datenänderung oder reiner Prüflauf. Auch danach muss die alte Version noch weiterlaufen können. **Jede datenändernde Migration endet mit einem Prüflauf in derselben Migration.**
3. **DDL mit Restriktionen:** neue Constraints passend zum geänderten Bestand. Alte Objekte löschst du nur, wenn ihr Inhalt verlustfrei übertragen wurde.

Leere Schritte entfallen. **Unzulässig** sind neue Constraints ohne vorherige Konsistenzprüfung des Bestands.

### Java-Migrationen
- Leite von **`BaseJavaMigration`** ab (**nicht** `BaselineJavaMigration`!) und implementiere `migrate(Context)`.
- Die Klasse ist eine Singleton-Bean über `@Component` ohne Scope-Annotation.
- Verwende JDBC oder ein `JdbcTemplate` **über die Connection aus dem Flyway-`Context`**.
- **Nicht** injizieren: `EntityManager`, `Connection`, `DataSource`, `JdbcTemplate`
- Keine eigenen Commits

## Flyway-Konfiguration (Beispiel)
```yaml
spring:
  flyway:
    table: DUMMY_SERVICE_FLYWAY_SCHEMA_HISTORY
    user: ${dummy.db.user}
    password: ${dummy.db.password}
    url: ${dummy.db.url}
    baseline-on-migrate: true
    baseline-version: 0.9.9
    default-schema: ${dummy.db.schema}
    schemas:
      - ${dummy.db.schema}
  datasource:
    username: ${dummy.db.user}
    url: ${dummy.db.url}
    password: ${dummy.db.password}
    hikari:
      schema: ${dummy.db.schema}
      minimum-idle: ${dummy.db.minimum-idle:1}
      maximum-pool-size: ${dummy.db.maximum-pool-size:5}
      idle-timeout: ${dummy.db.idle-timeout-millis:60000}
      max-lifetime: ${dummy.db.max-lifetime-millis}
      register-mbeans: ${dummy.db.register-mbeans:false}
```
- Flyway und der Betrieb nutzen derzeit **denselben DB-User**.
- Zeitwerte bekommen das Suffix **`-millis`** im Property-Namen.
- Pool-Parameter bekommen sinnvolle Defaults, damit die Betriebskonfiguration schlank bleibt.

## Lokaler Start ohne echte DB
Für den **lokalen Entwicklerstart** der Anwendung darf eine HSQL-Datenbank (In-Memory oder dateibasiert) genutzt werden, z. B. mit `--spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create`.
Das ist **kein Ersatz für Tests**. Für Tests gelten die Regeln in `AGENTS.md`, also keine In-Memory-DB-Tests.
