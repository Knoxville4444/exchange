# Logging (Log4j2)

## Abhängigkeiten
**Lib-Projekte:**
```gradle
implementation libs.bundles.log4j
```

**Service-Projekte:**
```gradle
implementation libs.spring.boot.starter.log4j2
```
Zusätzlich brauchst du die Ersetzungsregel, um Konflikte mit Logback zu vermeiden:
```gradle
modules {
    module("org.springframework.boot:spring-boot-starter-logging") {
        replacedBy("org.springframework.boot:spring-boot-starter-log4j2", 'Konfliktauflösung log4j')
    }
}
```

## Achtung: Endlosrekursion
`org.apache.logging.log4j:log4j-to-slf4j` darf **nicht** im Classpath landen.
Sie leitet Log4j2 nach SLF4J um. Zusammen mit dem Gegenstück `log4j-slf4j2-impl`, das SLF4J nach Log4j2 leitet, entsteht eine Endlosschleife.
- Normalerweise reicht die Ersetzungsregel oben.
- Eine manuelle Exklusion ist nur die Ultima Ratio und braucht **Rückfrage**.
- Prüfen kannst du das mit `gradlew dependencies --configuration runtimeClasspath | grep log4j-to-slf4j`.

## Logger im Code
```java
private final Logger log = LogManager.getLogger(getClass());   // nicht static
```
Alternativ geht `@Log4j2` auf Klassenebene, aber nur wenn `alias(libs.plugins.lombok)` im Projekt aktiv ist. Führe Lombok nicht eigenmächtig ein.

## Nachrichten
Nutze Lambdas bzw. MessageSupplier statt `isDebugEnabled()`-Abfragen, besonders in Schleifen:
```java
log.trace(() -> "Number is " + getRandomNumber());
log.trace("Number is {}", () -> getRandomNumber());
```

## Konfiguration
Eine explizite `log4j2.xml` setzt du bei Spring-Projekten per JVM-Option:
```
-Dlogging.config=file:"<Pfad_zu_log4j2.xml>"
```

## Datenschutz
| Erlaubt | Nicht erlaubt |
|---|---|
| **Technische IDs** (bevorzugt) | **Namen** und **Adressen** von Personen |
| **Aktenzeichen** | sonstige personenbezogene Daten |
| | Zugangsdaten, Tokens, Passwörter |
| | vollständige Request- und Response-Bodies |

- Um einen Vorgang wiederzufinden, loggst du bevorzugt technische IDs. Ein Aktenzeichen ist zulässig, wenn es den Kontext klarer macht.
- Enthält ein Objekt Namen oder Adressen, loggst du es **nicht** komplett, also kein `log.info("{}", antrag)` mit `toString()`. Gib stattdessen gezielt die ID aus.
