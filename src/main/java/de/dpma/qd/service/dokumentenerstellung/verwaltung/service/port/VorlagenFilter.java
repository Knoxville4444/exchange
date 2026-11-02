package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;

import java.util.Set;

/**
 * Suchkriterien für Vorlagen. Kriterien werden UND-verknüpft, Werte innerhalb eines Kriteriums ODER-verknüpft;
 * leere Mengen bzw. {@code null} schränken nicht ein.
 *
 * @param suchtext Teiltreffer in Name, Bezeichnung oder Beschreibung, ohne Groß-/Kleinschreibung
 */
public record VorlagenFilter(Set<String> kategorien, Set<VorlagenTyp> typen, Set<String> tags, String suchtext) {

    public VorlagenFilter {
        kategorien = kategorien == null ? Set.of() : Set.copyOf(kategorien);
        typen = typen == null ? Set.of() : Set.copyOf(typen);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        suchtext = suchtext == null || suchtext.isBlank() ? null : suchtext.trim();
    }

    public static VorlagenFilter alle() {
        return new VorlagenFilter(null, null, null, null);
    }
}
