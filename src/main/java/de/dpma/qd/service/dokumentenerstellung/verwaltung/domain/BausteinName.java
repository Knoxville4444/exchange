package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;

/**
 * Vollqualifizierter Name eines Bausteins {@code <KATEGORIE>/<Name>}. Im Formular-ODT steht er als
 * Name eines LibreOffice-Bereichs mit Präfix {@value #BEREICH_PRAEFIX}.
 */
public record BausteinName(String kategorie, String name) implements Comparable<BausteinName> {

    public static final String BEREICH_PRAEFIX = "baustein:";

    public BausteinName {
        Bezeichner.pruefeKategorie(kategorie);
        Bezeichner.pruefeName(name);
    }

    public static BausteinName parse(String text) {
        int i = text == null ? -1 : text.indexOf('/');
        if (i < 0) {
            throw new UngueltigeAnfrageException("BAUSTEIN_UNGUELTIG", "Baustein muss die Form KATEGORIE/Name haben: " + text);
        }
        return new BausteinName(text.substring(0, i), text.substring(i + 1));
    }

    /** Liest den Bausteinnamen aus einem Bereichsnamen, z. B. {@code baustein:PATGBM/Briefkopf}. */
    public static BausteinName ausBereich(String bereichsname) {
        if (bereichsname == null || !bereichsname.startsWith(BEREICH_PRAEFIX)) {
            throw new UngueltigeAnfrageException("BAUSTEIN_UNGUELTIG", "Kein Baustein-Bereich: " + bereichsname);
        }
        return parse(bereichsname.substring(BEREICH_PRAEFIX.length()));
    }

    public static boolean istBausteinBereich(String bereichsname) {
        return bereichsname != null && bereichsname.startsWith(BEREICH_PRAEFIX);
    }

    public String bereichsname() {
        return BEREICH_PRAEFIX + this;
    }

    @Override
    public int compareTo(BausteinName o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return kategorie + "/" + name;
    }
}
