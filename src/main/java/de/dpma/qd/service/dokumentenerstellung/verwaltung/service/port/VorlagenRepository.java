package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;

import java.util.List;
import java.util.Optional;

/**
 * Port für die Ablage der Vorlagen-Aggregate (Stammdaten, Versionen, Baustein-Referenzen – ohne Inhalte).
 */
public interface VorlagenRepository {

    Optional<Vorlage> finde(String kategorie, String name);

    /**
     * Lädt eine Vorlage mit Schreibsperre ({@code SELECT … FOR UPDATE}). Pflicht für alle ändernden
     * Operationen, damit z. B. zwei gleichzeitige Uploads nicht dieselbe Versionsnummer vergeben.
     */
    Optional<Vorlage> findeZumAendern(String kategorie, String name);

    List<Vorlage> suche(VorlagenFilter filter);

    /** Alle Formulare, von denen mindestens eine Version den Baustein referenziert. */
    List<Vorlage> findeReferenzierende(BausteinName baustein);

    /** Speichert das Aggregat und liefert es mit vergebenen IDs zurück. */
    Vorlage speichere(Vorlage vorlage);

    void loesche(Vorlage vorlage);
}
