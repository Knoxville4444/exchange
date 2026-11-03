package de.dpma.qd.service.dokumentenerstellung.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieRepository;

/** Kategorien (≙ Fachsysteme) auflisten, anlegen bzw. ändern und löschen. */
@Service
@Transactional
public class KategorieService {

    private final KategorieRepository kategorien;
    private final AuditErzeuger audit;

    public KategorieService(KategorieRepository kategorien, AuditErzeuger audit) {
        this.kategorien = kategorien;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<KategorieAnsicht> alle() {
        Map<String, Long> anzahl = anzahlVorlagen();
        return kategorien.findAll().stream()
                .map(KategorieEntityMapper::zuFachobjekt)
                .sorted(Comparator.comparing(Kategorie::name))
                .map(k -> new KategorieAnsicht(k, anzahl.getOrDefault(k.name(), 0L)))
                .toList();
    }

    /** Legt eine Kategorie an oder ändert ihre Beschreibung (idempotent). */
    public KategorieAnsicht speichere(String name, String beschreibung) {
        KategorieEntity entity = kategorien.findByName(name).orElseGet(KategorieEntity::new);
        Kategorie kategorie = entity.getId() == null
                ? new Kategorie(name, beschreibung, audit.neu())
                : new Kategorie(name, beschreibung, audit.geaendert(KategorieEntityMapper.zuFachobjekt(entity).audit()));
        KategorieEntityMapper.uebertrage(kategorie, entity);
        Kategorie gespeichert = KategorieEntityMapper.zuFachobjekt(kategorien.saveAndFlush(entity));
        return new KategorieAnsicht(gespeichert, anzahlVorlagen().getOrDefault(name, 0L));
    }

    public void loesche(String name) {
        KategorieEntity entity = kategorien.findByName(name)
                .orElseThrow(() -> FachlicherFehler.nichtGefunden("KATEGORIE_NICHT_GEFUNDEN", "Kategorie " + name + " nicht gefunden"));
        if (anzahlVorlagen().getOrDefault(name, 0L) > 0) {
            throw FachlicherFehler.konflikt("KATEGORIE_NICHT_LEER", "Kategorie " + name + " enthält noch Vorlagen");
        }
        kategorien.delete(entity);
    }

    private Map<String, Long> anzahlVorlagen() {
        Map<String, Long> anzahl = new HashMap<>();
        for (Object[] zeile : kategorien.zaehleVorlagenJeKategorie()) {
            anzahl.put((String) zeile[0], ((Number) zeile[1]).longValue());
        }
        return anzahl;
    }
}
