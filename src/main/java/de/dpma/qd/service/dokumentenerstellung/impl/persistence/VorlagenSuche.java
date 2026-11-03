package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenTyp;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/**
 * Suchkriterien für Vorlagen als JPA-Specification. Kriterien sind UND-, Werte innerhalb eines Kriteriums ODER-verknüpft;
 * leere Mengen bzw. {@code null} schränken nicht ein.
 */
public final class VorlagenSuche {

    private VorlagenSuche() {
    }

    public static Specification<VorlageEntity> nach(Set<String> kategorien, Set<VorlagenTyp> typen, Set<String> tags, String suchtext) {
        return (root, query, cb) -> {
            List<Predicate> bedingungen = new ArrayList<>();
            if (kategorien != null && !kategorien.isEmpty()) {
                bedingungen.add(root.get("kategorie").get("name").in(kategorien));
            }
            if (typen != null && !typen.isEmpty()) {
                bedingungen.add(root.get("typ").in(typen));
            }
            if (tags != null && !tags.isEmpty()) {
                Join<VorlageEntity, String> tag = root.join("tags");
                bedingungen.add(tag.in(tags));
                if (query != null) {
                    query.distinct(true);
                }
            }
            if (suchtext != null && !suchtext.isBlank()) {
                String muster = "%" + suchtext.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                bedingungen.add(cb.or(
                        cb.like(cb.lower(root.get("name")), muster, '\\'),
                        cb.like(cb.lower(root.get("bezeichnung")), muster, '\\'),
                        cb.like(cb.lower(root.get("beschreibung")), muster, '\\')));
            }
            return cb.and(bedingungen.toArray(Predicate[]::new));
        };
    }
}
