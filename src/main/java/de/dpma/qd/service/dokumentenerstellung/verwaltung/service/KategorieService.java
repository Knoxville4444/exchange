package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Kategorie;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.KategorieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Anwendungsfälle rund um Kategorien (≙ Fachsysteme). */
@Service
@Transactional
public class KategorieService {

    private final KategorieRepository kategorien;
    private final VorlagenZugriff zugriff;

    public KategorieService(KategorieRepository kategorien, VorlagenZugriff zugriff) {
        this.kategorien = kategorien;
        this.zugriff = zugriff;
    }

    @Transactional(readOnly = true)
    public List<KategorieAnsicht> alle() {
        Map<String, Long> anzahl = kategorien.anzahlVorlagen();
        return kategorien.alle().stream()
                .sorted(Comparator.comparing(Kategorie::name))
                .map(k -> new KategorieAnsicht(k, anzahl.getOrDefault(k.name(), 0L)))
                .toList();
    }

    /** Legt eine Kategorie an oder ändert ihre Beschreibung (idempotent). */
    public KategorieAnsicht speichere(String name, String beschreibung) {
        Kategorie kategorie = kategorien.finde(name)
                .map(k -> k.mitBeschreibung(beschreibung, zugriff.geaendert(k.audit())))
                .orElseGet(() -> new Kategorie(null, name, beschreibung, zugriff.neuesAudit()));
        Kategorie gespeichert = kategorien.speichere(kategorie);
        return new KategorieAnsicht(gespeichert, kategorien.anzahlVorlagen().getOrDefault(name, 0L));
    }

    public void loesche(String name) {
        kategorien.finde(name).orElseThrow(() -> new NichtGefundenException("KATEGORIE_NICHT_GEFUNDEN",
                "Kategorie " + name + " nicht gefunden"));
        if (kategorien.anzahlVorlagen().getOrDefault(name, 0L) > 0) {
            throw new KonfliktException("KATEGORIE_NICHT_LEER", "Kategorie " + name + " enthält noch Vorlagen");
        }
        kategorien.loesche(name);
    }
}
