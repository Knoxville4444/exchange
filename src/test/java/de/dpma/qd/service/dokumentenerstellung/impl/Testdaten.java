package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Testdaten für Fachobjekte. */
public final class Testdaten {

    public static final Instant T0 = Instant.parse("2026-12-01T10:00:00Z");
    public static final BausteinName BRIEFKOPF = new BausteinName("PATGBM", "Briefkopf");

    private static final AtomicLong IDS = new AtomicLong(1000);

    private Testdaten() {
    }

    public static Instant tage(long anzahl) {
        return T0.plus(Duration.ofDays(anzahl));
    }

    public static VersionsNummer v(String text) {
        return VersionsNummer.parse(text);
    }

    public static VersionsAngabe angabe(String text) {
        return VersionsAngabe.parse(text);
    }

    public static Audit audit() {
        return Audit.neu(T0, "tester");
    }

    public static InhaltRef odt() {
        return inhalt(Inhaltsformat.ODT);
    }

    public static InhaltRef xsd() {
        return inhalt(Inhaltsformat.XSD);
    }

    public static InhaltRef inhalt(Inhaltsformat format) {
        long id = IDS.incrementAndGet();
        return new InhaltRef(id, "sha-" + id, format, 100);
    }

    /** Formularversion im Status DRAFT (mit Schema). */
    public static Version formularVersion(String nummer, BausteinReferenz... bausteine) {
        return Version.entwurf(v(nummer), null, odt(), xsd(), List.of(bausteine), audit());
    }

    /** Bausteinversion im Status DRAFT. */
    public static Version bausteinVersion(String nummer) {
        return Version.entwurf(v(nummer), null, odt(), null, List.of(), audit());
    }

    public static Version veroeffentlicht(Version version, Instant ab, Instant bis) {
        return version.freigegeben(new Zeitraum(ab, bis), audit());
    }

    public static BausteinReferenz briefkopf(String angabe) {
        return new BausteinReferenz(BRIEFKOPF, angabe(angabe));
    }

    public static Vorlage formular(String name, Version... versionen) {
        return new Vorlage("PATGBM", name, VorlagenTyp.FORMULAR, "Formular " + name, null, Set.of(), null, audit(), List.of(versionen));
    }

    public static Vorlage baustein(String name, Version... versionen) {
        return new Vorlage("PATGBM", name, VorlagenTyp.BAUSTEIN, "Baustein " + name, null, Set.of(), null, audit(), List.of(versionen));
    }
}
