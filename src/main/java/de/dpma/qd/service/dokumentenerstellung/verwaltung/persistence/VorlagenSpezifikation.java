package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenFilter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Übersetzt den {@link VorlagenFilter} in eine JPA-Criteria-Abfrage. */
final class VorlagenSpezifikation {

    private VorlagenSpezifikation() {
    }

    static Specification<VorlageEntity> aus(VorlagenFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> bedingungen = new ArrayList<>();
            if (!filter.kategorien().isEmpty()) {
                bedingungen.add(root.get("kategorie").get("name").in(filter.kategorien()));
            }
            if (!filter.typen().isEmpty()) {
                bedingungen.add(root.get("typ").in(filter.typen()));
            }
            if (!filter.tags().isEmpty()) {
                Join<VorlageEntity, String> tag = root.join("tags");
                bedingungen.add(tag.in(filter.tags()));
                if (query != null) {
                    query.distinct(true);
                }
            }
            if (filter.suchtext() != null) {
                String muster = "%" + filter.suchtext().toLowerCase(Locale.ROOT)
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
