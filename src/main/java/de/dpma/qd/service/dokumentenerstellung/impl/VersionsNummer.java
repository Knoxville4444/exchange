package de.dpma.qd.service.dokumentenerstellung.impl;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exakte Versionsnummer {@code <Hauptversion>.<Nebenversion>}, z. B. {@code 2.3}.
 * <p>
 * Der Vergleich ist numerisch, d. h. {@code 1.10 > 1.9}.
 */
public record VersionsNummer(int haupt, int neben) implements Comparable<VersionsNummer> {

    private static final Pattern FORMAT = Pattern.compile("([1-9]\\d{0,4})\\.(\\d{1,5})");

    public VersionsNummer {
        if (haupt < 1) {
            throw FachlicherFehler.ungueltig("VERSION_UNGUELTIG", "Hauptversion muss mindestens 1 sein: " + haupt);
        }
        if (neben < 0) {
            throw FachlicherFehler.ungueltig("VERSION_UNGUELTIG", "Nebenversion darf nicht negativ sein: " + neben);
        }
    }

    public static VersionsNummer parse(String text) {
        Matcher m = FORMAT.matcher(text == null ? "" : text.trim());
        if (!m.matches()) {
            throw FachlicherFehler.ungueltig("VERSION_UNGUELTIG",
                    "Version muss das Format <Hauptversion>.<Nebenversion> haben: " + text);
        }
        return new VersionsNummer(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
    }

    public static VersionsNummer erste() {
        return new VersionsNummer(1, 0);
    }

    public boolean istHauptversion() {
        return neben == 0;
    }

    public VersionsNummer naechsteNebenversion() {
        return new VersionsNummer(haupt, neben + 1);
    }

    public VersionsNummer naechsteHauptversion() {
        return new VersionsNummer(haupt + 1, 0);
    }

    @Override
    public int compareTo(VersionsNummer andere) {
        int c = Integer.compare(haupt, andere.haupt);
        return c != 0 ? c : Integer.compare(neben, andere.neben);
    }

    @Override
    public String toString() {
        return haupt + "." + neben;
    }
}
