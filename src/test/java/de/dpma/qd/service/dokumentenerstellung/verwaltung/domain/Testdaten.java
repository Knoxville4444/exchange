package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Bausteine für Domänentests. */
public final class Testdaten {

    public static final Instant T0 = Instant.parse("2026-12-01T10:00:00Z");

    private static long naechsteId = 100;

    private Testdaten() {
    }

    public static Instant tage(long anzahl) {
        return T0.plus(Duration.ofDays(anzahl));
    }

    public static InhaltRef odt() {
        long id = naechsteId++;
        return new InhaltRef(id, "sha-" + id, Inhaltsformat.ODT, 1000);
    }

    public static InhaltRef xsd() {
        long id = naechsteId++;
        return new InhaltRef(id, "sha-" + id, Inhaltsformat.XSD, 200);
    }

    public static InhaltRef pdf() {
        long id = naechsteId++;
        return new InhaltRef(id, "sha-" + id, Inhaltsformat.PDF, 5000);
    }

    public static Audit audit() {
        return Audit.neu(T0, "tester");
    }

    public static Vorlage formular(String name, BausteinReferenz... bausteine) {
        return Vorlage.neu("PATGBM", name, VorlagenTyp.FORMULAR, "Formular " + name, null, Set.of(), null,
                odt(), xsd(), List.of(bausteine), audit());
    }

    public static Vorlage baustein(String name) {
        return Vorlage.neu("PATGBM", name, VorlagenTyp.BAUSTEIN, "Baustein " + name, null, Set.of(), null,
                odt(), null, List.of(), audit());
    }

    /** Legt eine Nebenversion (Kopie) an und liefert ihre Nummer. */
    public static VersionsNummer neben(Vorlage vorlage) {
        Vorlage.VersionsPlan plan = vorlage.planeNeueVersion(VersionArt.NEBEN, null, null);
        Version basis = plan.basis().orElseThrow();
        vorlage.fuegeVersionHinzu(plan.nummer(), null, basis.inhalt(), basis.schema(), basis.bausteine(), audit());
        return plan.nummer();
    }

    /** Legt eine Hauptversion (Kopie, neues Schema bei Formularen) an und liefert ihre Nummer. */
    public static VersionsNummer haupt(Vorlage vorlage) {
        Vorlage.VersionsPlan plan = vorlage.planeNeueVersion(VersionArt.HAUPT, null, null);
        Version basis = plan.basis().orElseThrow();
        vorlage.fuegeVersionHinzu(plan.nummer(), null, basis.inhalt(), vorlage.typ().istFormular() ? xsd() : null,
                basis.bausteine(), audit());
        return plan.nummer();
    }

    public static VersionsNummer v(String text) {
        return VersionsNummer.parse(text);
    }

    public static VersionsAngabe angabe(String text) {
        return VersionsAngabe.parse(text);
    }
}
