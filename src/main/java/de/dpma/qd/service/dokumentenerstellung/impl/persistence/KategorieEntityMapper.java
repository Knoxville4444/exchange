package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import de.dpma.qd.service.dokumentenerstellung.impl.Audit;
import de.dpma.qd.service.dokumentenerstellung.impl.Kategorie;

/** Mapping Kategorie-Entity ↔ Fachobjekt. */
public final class KategorieEntityMapper {

    private KategorieEntityMapper() {
    }

    public static Kategorie zuFachobjekt(KategorieEntity e) {
        return new Kategorie(e.getName(), e.getBeschreibung(), zuFachobjekt(e.getAudit()));
    }

    public static void uebertrage(Kategorie kategorie, KategorieEntity e) {
        e.setName(kategorie.name());
        e.setBeschreibung(kategorie.beschreibung());
        e.setAudit(zuEntity(kategorie.audit()));
    }

    static Audit zuFachobjekt(AuditEmbeddable e) {
        return new Audit(e.getErstelltAm(), e.getErstelltVon(), e.getGeaendertAm(), e.getGeaendertVon());
    }

    static AuditEmbeddable zuEntity(Audit a) {
        return new AuditEmbeddable(a.erstelltAm(), a.erstelltVon(), a.geaendertAm(), a.geaendertVon());
    }
}
