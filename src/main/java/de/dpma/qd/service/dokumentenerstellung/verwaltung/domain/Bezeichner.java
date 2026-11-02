package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;

import java.util.regex.Pattern;

/** Formatregeln für Kategorie- und Vorlagennamen (identisch zur OpenAPI). */
public final class Bezeichner {

    public static final Pattern KATEGORIE = Pattern.compile("[A-Z][A-Z0-9_]{1,29}");
    public static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,100}");

    private Bezeichner() {
    }

    public static String pruefeKategorie(String kategorie) {
        if (kategorie == null || !KATEGORIE.matcher(kategorie).matches()) {
            throw new UngueltigeAnfrageException("KATEGORIE_UNGUELTIG", "Ungültiger Kategoriename: " + kategorie);
        }
        return kategorie;
    }

    public static String pruefeName(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new UngueltigeAnfrageException("NAME_UNGUELTIG", "Ungültiger Vorlagenname: " + name);
        }
        return name;
    }
}
