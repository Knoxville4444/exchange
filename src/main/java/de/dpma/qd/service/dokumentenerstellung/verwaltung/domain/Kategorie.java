package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

/** Kategorie einer Vorlage; entspricht dem Fachsystem. */
public record Kategorie(Long id, String name, String beschreibung, Audit audit) {

    public Kategorie {
        Bezeichner.pruefeKategorie(name);
    }

    public Kategorie mitBeschreibung(String neueBeschreibung, Audit neuesAudit) {
        return new Kategorie(id, name, neueBeschreibung, neuesAudit);
    }
}
