package de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenFilter;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Repository im Speicher. Speichert und liefert <em>Kopien</em> der Aggregate, damit sich – wie mit einer
 * Datenbank – nicht gespeicherte Änderungen nach einer Ausnahme nicht auswirken.
 */
public final class InMemoryVorlagenRepository implements VorlagenRepository {

    private final Map<String, Vorlage> vorlagen = new TreeMap<>();
    private long naechsteId = 1;

    @Override
    public Optional<Vorlage> finde(String kategorie, String name) {
        return Optional.ofNullable(vorlagen.get(kategorie + "/" + name)).map(v -> kopie(v, false));
    }

    @Override
    public Optional<Vorlage> findeZumAendern(String kategorie, String name) {
        return finde(kategorie, name);
    }

    @Override
    public List<Vorlage> suche(VorlagenFilter f) {
        return vorlagen.values().stream()
                .filter(v -> f.kategorien().isEmpty() || f.kategorien().contains(v.kategorie()))
                .filter(v -> f.typen().isEmpty() || f.typen().contains(v.typ()))
                .filter(v -> f.tags().isEmpty() || v.tags().stream().anyMatch(f.tags()::contains))
                .filter(v -> f.suchtext() == null || (v.name() + " " + v.bezeichnung() + " " + v.beschreibung())
                        .toLowerCase().contains(f.suchtext().toLowerCase()))
                .map(v -> kopie(v, false))
                .toList();
    }

    @Override
    public List<Vorlage> findeReferenzierende(BausteinName baustein) {
        return vorlagen.values().stream()
                .filter(v -> v.versionen().stream().anyMatch(ver -> ver.referenzAuf(baustein).isPresent()))
                .map(v -> kopie(v, false))
                .toList();
    }

    @Override
    public Vorlage speichere(Vorlage vorlage) {
        Vorlage gespeichert = kopie(vorlage, true);
        vorlagen.put(gespeichert.vollerName(), gespeichert);
        return kopie(gespeichert, false);
    }

    @Override
    public void loesche(Vorlage vorlage) {
        vorlagen.remove(vorlage.vollerName());
    }

    public List<Vorlage> alle() {
        return List.copyOf(vorlagen.values());
    }

    private Vorlage kopie(Vorlage v, boolean idsVergeben) {
        List<Version> versionen = v.versionen().stream()
                .map(ver -> Version.rekonstruiere(ver.id() == null && idsVergeben ? naechsteId++ : ver.id(), ver.nummer(),
                        ver.status(), ver.zeitraum(), ver.kommentar(), ver.inhalt(), ver.schema(), ver.bausteine(), ver.audit()))
                .toList();
        Long id = v.id() == null && idsVergeben ? naechsteId++ : v.id();
        return Vorlage.rekonstruiere(id, v.kategorie(), v.name(), v.typ(), v.bezeichnung(), v.beschreibung(), v.tags(),
                v.veroeffentlichung(), v.audit(), v.revision(), versionen);
    }
}
