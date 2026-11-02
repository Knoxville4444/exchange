package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Kategorie;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Port für die Ablage der Kategorien. */
public interface KategorieRepository {

    List<Kategorie> alle();

    Optional<Kategorie> finde(String name);

    Kategorie speichere(Kategorie kategorie);

    void loesche(String name);

    /** Anzahl der Vorlagen je Kategoriename (Kategorien ohne Vorlagen mit 0). */
    Map<String, Long> anzahlVorlagen();
}
