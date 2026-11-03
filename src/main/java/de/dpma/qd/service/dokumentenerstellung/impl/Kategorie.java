package de.dpma.qd.service.dokumentenerstellung.impl;

/** Kategorie einer Vorlage; entspricht dem Fachsystem. */
public record Kategorie(String name, String beschreibung, Audit audit) {

    public Kategorie {
        Bezeichner.pruefeKategorie(name);
    }
}
