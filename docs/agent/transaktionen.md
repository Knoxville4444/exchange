# Verteilte Transaktionalität

Lies diese Datei, wenn ein Ablauf **mehrere Systeme** betrifft (eigene DB + externer Service, DMS, Messaging).
Welches Pattern passt, entscheidest du **nicht allein**. Schlag es vor und begründe es.

## Idempotenz (serverseitige Deduplizierung)
Mehrfaches Senden desselben Requests hat dieselbe Wirkung wie einmaliges Senden.
- **`PUT` / `DELETE`** sind idempotent. Die Ressource muss im Pfad eindeutig identifiziert sein. Bei `PUT` unterscheidest du „anlegen“ (existiert nicht) von „ändern“ (existiert).
- **`PATCH`** ist nicht zwingend idempotent. Optimistic Locking über Versionsangaben ist möglich, bei Bedarf auch Idempotenzschlüssel.
- **`POST`, asynchrone Abläufe, Messaging:** Aus den relevanten Daten des logischen Requests wird ein **Hash als Idempotenzschlüssel** gebildet und mit der Entität gespeichert. Vor dem Anlegen prüfst du, ob es zum Schlüssel schon eine Entität gibt. Wenn ja, gibst du diese zurück.
  Serverseitig erzeugte Schlüssel setzen voraus, dass ein Client nie zwei inhaltsgleiche Requests in unterschiedlichem Kontext sendet, oder dass die Infrastruktur ein Unterscheidungsmerkmal liefert (z. B. eine JMS-Message-ID).

## Statemachine (clientseitig gegenüber externen Systemen, mit eigener DB)
- Die Entität durchläuft definierte Zustände. Ein Schritt besteht aus einem ändernden Call ans externe System und danach einem Update in der eigenen DB.
- Der externe Call ist idealerweise idempotent. Wenn nicht, **suchst du vorher**, ob die Operation schon ausgeführt wurde.
  - Die Suche darf nur übersprungen werden, wenn sicher kein Retry vorliegt. Sie muss trotzdem implementiert sein.
  - Mehrfachausführung ist nur dann unkritisch (z. B. „nur Datenmüll“), wenn das **vorher mit dem Systemarchitekten abgestimmt** ist.
- Vom externen System erzeugte IDs speicherst du zusammen mit dem Statuswechsel in **einer** lokalen Transaktion.
- Vorteil: Das Ganze ist ohne DB und ohne Spring-Kontext unit-testbar. Das passt zu unserer Teststrategie.
- Bei einer Prozessengine ist zu beachten, dass deren eigene Persistenz mitbetrachtet werden muss.

## Transactional Outbox (Dual Write absichern)
Zwei Datenstände müssen konsistent geändert werden, z. B. ein DB-Satz und ein DMS-Dokument.
- In **einer** `@Transactional`-Methode änderst du den ersten Datensatz und publizierst ein Event (einfacher `record`).
- Ein Handler arbeitet das Event asynchron ab. Bei einem Fehler bleibt es stehen und wird periodisch erneut versucht.
- Umsetzung mit **Spring Modulith**: `org.springframework.modulith:spring-modulith-starter-jdbc`. Das ist eine neue Abhängigkeit und braucht deshalb Rückfrage.
- Damit Modulith nicht jedes Sub-Package zu einem Modul macht, setzt du in der `application.yml`:
  ```yaml
  spring:
    modulith:
      detection-strategy: explicitly-annotated
  ```
- Referenzbeispiel: `faktura-backend-service` (`RechnungService`, `OutboxEventHandler`)
