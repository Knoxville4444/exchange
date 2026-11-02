package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Kategorie;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.KategorieRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adapter: {@link KategorieRepository} auf Oracle via JPA. */
@Repository
class JpaKategorieRepository implements KategorieRepository {

    private final KategorieJpaRepository kategorien;
    private final VorlageJpaRepository vorlagen;

    JpaKategorieRepository(KategorieJpaRepository kategorien, VorlageJpaRepository vorlagen) {
        this.kategorien = kategorien;
        this.vorlagen = vorlagen;
    }

    @Override
    public List<Kategorie> alle() {
        return kategorien.findAll().stream().map(PersistenzMapper::zuDomain).toList();
    }

    @Override
    public Optional<Kategorie> finde(String name) {
        return kategorien.findByName(name).map(PersistenzMapper::zuDomain);
    }

    @Override
    public Kategorie speichere(Kategorie kategorie) {
        KategorieEntity e = kategorien.findByName(kategorie.name()).orElseGet(KategorieEntity::new);
        e.setName(kategorie.name());
        e.setBeschreibung(kategorie.beschreibung());
        e.setAudit(PersistenzMapper.zuEntity(kategorie.audit()));
        return PersistenzMapper.zuDomain(kategorien.saveAndFlush(e));
    }

    @Override
    public void loesche(String name) {
        kategorien.findByName(name).ifPresent(kategorien::delete);
    }

    @Override
    public Map<String, Long> anzahlVorlagen() {
        Map<String, Long> anzahl = new HashMap<>();
        for (Object[] zeile : vorlagen.anzahlJeKategorie()) {
            anzahl.put((String) zeile[0], ((Number) zeile[1]).longValue());
        }
        return anzahl;
    }
}
