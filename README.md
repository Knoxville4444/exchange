# Testdokument-Generator

Erzeugt mehrseitige Test-PDFs für die Digitalisierungs-Anwendung – ohne echte Scandaten.
Formulare aus einem lokalen Verzeichnis werden per Klick/Drag&Drop mit generierten
Trennblättern (Paket, Dokument, ZBH) und Dummy-Seiten zu einem Gesamtdokument
zusammengestellt und als PDF heruntergeladen oder gedruckt.

## Benutzung

`index.html` im Browser öffnen (Doppelklick reicht – kein Server, keine Installation,
keine Admin-Rechte nötig). Empfohlen: **Firefox**. Chrome funktioniert ebenfalls.

1. **Quellen**-Tab: "Verzeichnis wählen…" klickt sich zu einem Ordner mit PDF-Formularen
   durch. Dateien werden automatisch nach Schutzrecht gruppiert (Marke, Patente /
   Gebrauchsmuster, Design, Zahlungsverkehr – anhand der Ordnernamen `Marken`,
   `Patente`, `Gebrauchsmuster`, `Designs`, `Zahlungsverkehr`). Klick oder Drag auf eine
   Karte fügt das ganze PDF als Seitenstapel in den Streifen ein.
2. **Generator**-Tab: erzeugt auf Zuruf
   - **Pakettrennblatt** (optional mit Dokumentenkategorie)
   - **Dokumenttrennblatt** (optional mit Dokumenttyp, gefiltert nach Fachsystem)
   - **ZBH** (statisches Zusatzbehandlung-Trennblatt, keine Eingaben)
   - **Dummy-Seite(n)** (Seitenzahl-Stempel oder Lorem-Text, frei wählbare Anzahl)
3. Der **Streifen** in der Mitte wächst horizontal und gruppiert alles automatisch
   sichtbar nach Paket/Dokument – auch lose Seiten vor dem ersten Pakettrennblatt
   werden als "Paket (unbenannt)" dargestellt. Karten lassen sich per Drag&Drop
   umsortieren; Hover zeigt ein × zum Löschen einzelner Seiten oder ganzer
   Paket-/Dokument-Gruppen.
4. Die **Gliederung** links zeigt dieselbe Struktur als Baum zur schnellen Navigation.
5. **Download** lädt das fertige PDF herunter, **Drucken** öffnet es in einem neuen Tab
   zum Ausdrucken über den Browser (z. B. als PDF-Drucker).

## Struktur

```
index.html       Oberfläche
app.js           UI-Logik (Quellen, Generator, Streifen, Baum, Export)
pdfgen.js        PDF-Erzeugung: Trennblätter, Dummy-Seiten, finaler Zusammenbau
styles.css       Layout
lib/             Lokal eingebundene Bibliotheken (pdf-lib, PDF.js 2.16, QR-Generator)
data/
  doktypen.json                   Dokumenttypen je Fachsystem (editierbar)
  dokumentkategorien.json         Kategorien fürs Pakettrennblatt (editierbar)
  doktyp-formular-mapping.json    Platzhalter für Doktyp↔Formularname-Zuordnung (noch leer)
  data.js                         Generiert aus den beiden .json-Dateien oben –
                                   nach Änderungen dort neu erzeugen (siehe unten)
```

### `data.js` neu erzeugen

Nach Änderungen an `doktypen.json` oder `dokumentkategorien.json`:

```bash
python3 -c "
import json
doktypen = json.load(open('data/doktypen.json', encoding='utf-8'))
kategorien = json.load(open('data/dokumentkategorien.json', encoding='utf-8'))
with open('data/data.js', 'w', encoding='utf-8') as f:
    f.write('window.DOKTYPEN = ')
    json.dump(doktypen, f, ensure_ascii=False)
    f.write(';\nwindow.DOKUMENTKATEGORIEN = ')
    json.dump(kategorien, f, ensure_ascii=False)
    f.write(';\n')
"
```

## Technische Hinweise

- Läuft bewusst rein über `file://` (klassische `<script>`-Tags statt ES-Module, Daten
  inline statt per `fetch()`) – funktioniert dadurch auch auf abgesicherten
  Windows-Rechnern ohne Admin-Rechte und ohne Skript-/Programmausführung außerhalb
  freigegebener Verzeichnisse, da nur der bereits installierte Browser läuft.
- PDF.js ist bewusst auf Version 2.16 fixiert (letzte Version mit klassischem,
  nicht-modulbasiertem Build).
- QR-Codes werden als Vektor-Rechtecke gezeichnet (kein Rasterbild), Patch-Code-Rahmen
  sind eine vereinfachte, visuelle Annäherung – keine hardwaregenauen Kodak-Patchcodes.
- `doktyp-formular-mapping.json` ist vorbereitet, aber noch ohne Daten und ohne
  UI-Anbindung.
