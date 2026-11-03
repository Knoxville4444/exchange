package de.dpma.qd.service.dokumentenerstellung.impl;


import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Versionsangabe eines Aufrufers bzw. einer Baustein-Referenz.
 * <ul>
 *     <li>{@code 2}: neueste veröffentlichte Nebenversion der Hauptversion 2</li>
 *     <li>{@code 2.3}: exakt Version 2.3</li>
 * </ul>
 * Keine Angabe (alle Versionen) wird durch {@code null} bzw. {@link Optional#empty()} ausgedrückt.
 */
public record VersionsAngabe(int haupt, Integer neben) {

    private static final Pattern FORMAT = Pattern.compile("([1-9]\\d{0,4})(?:\\.(\\d{1,5}))?");

    public VersionsAngabe {
        if (haupt < 1 || (neben != null && neben < 0)) {
            throw FachlicherFehler.ungueltig("VERSION_UNGUELTIG", "Ungültige Versionsangabe: " + haupt + "." + neben);
        }
    }

    public static VersionsAngabe parse(String text) {
        Matcher m = FORMAT.matcher(text == null ? "" : text.trim());
        if (!m.matches()) {
            throw FachlicherFehler.ungueltig("VERSION_UNGUELTIG",
                    "Versionsangabe muss das Format <Hauptversion> oder <Hauptversion>.<Nebenversion> haben: " + text);
        }
        Integer neben = m.group(2) == null ? null : Integer.valueOf(m.group(2));
        return new VersionsAngabe(Integer.parseInt(m.group(1)), neben);
    }

    /** Liest eine optionale Angabe; leer oder {@code null} bedeutet „keine Einschränkung“. */
    public static Optional<VersionsAngabe> parseOptional(String text) {
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(parse(text));
    }

    public static VersionsAngabe hauptversion(int haupt) {
        return new VersionsAngabe(haupt, null);
    }

    public static VersionsAngabe exakt(VersionsNummer nummer) {
        return new VersionsAngabe(nummer.haupt(), nummer.neben());
    }

    public boolean istExakt() {
        return neben != null;
    }

    public boolean passtZu(VersionsNummer nummer) {
        return nummer.haupt() == haupt && (neben == null || nummer.neben() == neben);
    }

    @Override
    public String toString() {
        return neben == null ? String.valueOf(haupt) : haupt + "." + neben;
    }
}
