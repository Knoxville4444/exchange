package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinPruefung.FehlendeAufloesung;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.angabe;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.audit;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.baustein;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.formular;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.haupt;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.neben;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.tage;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BausteinPruefungTest {

    private static final BausteinName BRIEFKOPF = new BausteinName("PATGBM", "Briefkopf");

    @Test
    void formularIstVeroeffentlichbarWennDerBausteinAufloesbarIst() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        Vorlage f = formular("Bescheid", new BausteinReferenz(BRIEFKOPF, angabe("1")));
        f.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());

        assertTrue(BausteinPruefung.pruefeFormularVersion(f, f.version(v("1.0")), quelle(b), T0).isEmpty());
    }

    @Test
    void formularMitNurEntworfenemBausteinIstNichtVeroeffentlichbar() {
        Vorlage b = baustein("Briefkopf");                       // nur DRAFT
        Vorlage f = formular("Bescheid", new BausteinReferenz(BRIEFKOPF, angabe("1")));
        f.setzeVeroeffentlichung(v("1.0"), tage(10), null, T0, audit());

        List<FehlendeAufloesung> fehlend = BausteinPruefung.pruefeFormularVersion(f, f.version(v("1.0")), quelle(b), T0);
        assertEquals(1, fehlend.size());
        assertEquals(tage(10), fehlend.getFirst().zeitpunkt());
    }

    @Test
    void formularDarfErstAbDemBausteinterminGeplantWerden() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), tage(10), null, T0, audit());
        Vorlage f = formular("Bescheid", new BausteinReferenz(BRIEFKOPF, angabe("1")));

        f.setzeVeroeffentlichung(v("1.0"), tage(9), null, T0, audit());
        assertEquals(1, BausteinPruefung.pruefeFormularVersion(f, f.version(v("1.0")), quelle(b), T0).size());

        f.setzeVeroeffentlichung(v("1.0"), tage(10), null, T0, audit());
        assertTrue(BausteinPruefung.pruefeFormularVersion(f, f.version(v("1.0")), quelle(b), T0).isEmpty());
    }

    @Test
    void rueckzugMitLueckeWirdErkannt() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        VersionsNummer nachfolger = neben(b);
        b.setzeVeroeffentlichung(nachfolger, tage(20), null, T0, audit());
        Vorlage f = veroeffentlichtesFormularMit(angabe("1"));

        b.setzeVeroeffentlichung(v("1.0"), null, tage(15), T0, audit());     // Lücke 15–20
        List<FehlendeAufloesung> fehlend = BausteinPruefung.pruefeBausteinAenderung(b, List.of(f), T0);
        assertEquals(1, fehlend.size());
        assertEquals(tage(15), fehlend.getFirst().zeitpunkt());
    }

    @Test
    void rueckzugGenauZumStartDesNachfolgersIstErlaubt() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        VersionsNummer nachfolger = neben(b);
        b.setzeVeroeffentlichung(nachfolger, tage(20), null, T0, audit());
        Vorlage f = veroeffentlichtesFormularMit(angabe("1"));

        b.setzeVeroeffentlichung(v("1.0"), null, tage(20), T0, audit());
        assertTrue(BausteinPruefung.pruefeBausteinAenderung(b, List.of(f), T0).isEmpty());
    }

    @Test
    void exaktGepinnteBausteinversionDarfNichtZurueckgezogenWerden() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        VersionsNummer nachfolger = neben(b);
        b.setzeVeroeffentlichung(nachfolger, null, null, T0, audit());
        Vorlage f = veroeffentlichtesFormularMit(angabe("1.0"));

        b.setzeVeroeffentlichung(v("1.0"), null, tage(5), T0, audit());
        assertEquals(1, BausteinPruefung.pruefeBausteinAenderung(b, List.of(f), T0).size());
    }

    @Test
    void zurueckgezogeneFormulareBrauchenKeineBausteine() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        Vorlage f = formular("Bescheid", new BausteinReferenz(BRIEFKOPF, angabe("1")));
        f.setzeVeroeffentlichung(v("1.0"), null, tage(5), T0, audit());

        b.setzeVeroeffentlichung(v("1.0"), null, tage(5), T0, audit());
        assertTrue(BausteinPruefung.pruefeBausteinAenderung(b, List.of(f), T0).isEmpty());
    }

    @Test
    void neueHauptversionDesBausteinsErsetztDieAlteNicht() {
        Vorlage b = baustein("Briefkopf");
        b.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        VersionsNummer zwei = haupt(b);
        b.setzeVeroeffentlichung(zwei, null, null, T0, audit());
        Vorlage f = veroeffentlichtesFormularMit(angabe("1"));

        b.setzeVeroeffentlichung(v("1.0"), null, tage(1), T0, audit());
        assertEquals(1, BausteinPruefung.pruefeBausteinAenderung(b, List.of(f), T0).size(),
                "Formular nutzt Hauptversion 1 – 2.0 ist kein Ersatz");
    }

    // --- Abgleich der Referenzen ----------------------------------------------------------

    @Test
    void abgleichBehaeltBestehendeUndErgaenztNeueReferenzen() {
        Vorlage briefkopf = baustein("Briefkopf");
        haupt(briefkopf);                                            // 2.0
        Vorlage fusszeile = baustein("Fusszeile");
        BausteinName fuss = new BausteinName("PATGBM", "Fusszeile");
        BausteinName alt = new BausteinName("PATGBM", "Entfallen");

        List<BausteinReferenz> ergebnis = BausteinPruefung.gleicheAb(Set.of(BRIEFKOPF, fuss),
                List.of(new BausteinReferenz(BRIEFKOPF, angabe("1.0")), new BausteinReferenz(alt, angabe("1"))),
                quelle(briefkopf, fusszeile));

        assertEquals(List.of(new BausteinReferenz(BRIEFKOPF, angabe("1.0")), new BausteinReferenz(fuss, angabe("1"))), ergebnis);
    }

    @Test
    void neueReferenzZeigtAufDieHoechsteHauptversion() {
        Vorlage briefkopf = baustein("Briefkopf");
        haupt(briefkopf);
        List<BausteinReferenz> ergebnis = BausteinPruefung.gleicheAb(Set.of(BRIEFKOPF), List.of(), quelle(briefkopf));
        assertEquals(angabe("2"), ergebnis.getFirst().angabe());
    }

    @Test
    void unbekannterBausteinWirdAbgelehnt() {
        assertThrows(UngueltigeAnfrageException.class,
                () -> BausteinPruefung.gleicheAb(Set.of(BRIEFKOPF), List.of(), n -> Optional.empty()));
    }

    @Test
    void formularAlsBausteinWirdAbgelehnt() {
        Vorlage keinBaustein = formular("Briefkopf");
        assertThrows(UngueltigeAnfrageException.class,
                () -> BausteinPruefung.gleicheAb(Set.of(BRIEFKOPF), List.of(), n -> Optional.of(keinBaustein)));
    }

    @Test
    void gesetzteReferenzenMuessenZuDenBereichenPassen() {
        Vorlage b = baustein("Briefkopf");
        assertThrows(UngueltigeAnfrageException.class,
                () -> BausteinPruefung.pruefeReferenzen(Set.of(), List.of(new BausteinReferenz(BRIEFKOPF, angabe("1"))), quelle(b)));
        assertThrows(UngueltigeAnfrageException.class,
                () -> BausteinPruefung.pruefeReferenzen(Set.of(BRIEFKOPF), List.of(new BausteinReferenz(BRIEFKOPF, angabe("1.5"))), quelle(b)));
        assertEquals(1, BausteinPruefung.pruefeReferenzen(Set.of(BRIEFKOPF),
                List.of(new BausteinReferenz(BRIEFKOPF, angabe("1.0"))), quelle(b)).size());
    }

    @Test
    void loeschenDerLetztenPassendenVersionHinterlaesstVerwaisteReferenz() {
        Vorlage b = baustein("Briefkopf");
        VersionsNummer zwei = haupt(b);
        Vorlage f = formular("Bescheid", new BausteinReferenz(BRIEFKOPF, angabe("2")));
        b.loescheVersion(zwei);
        assertEquals(1, BausteinPruefung.verwaisteReferenzen(b, List.of(f)).size());
    }

    // -------------------------------------------------------------------------------------

    private static Vorlage veroeffentlichtesFormularMit(VersionsAngabe angabe) {
        Vorlage f = formular("Bescheid", new BausteinReferenz(BRIEFKOPF, angabe));
        f.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        return f;
    }

    private static Function<BausteinName, Optional<Vorlage>> quelle(Vorlage... bausteine) {
        Map<BausteinName, Vorlage> map = new java.util.HashMap<>();
        for (Vorlage b : bausteine) {
            map.put(new BausteinName(b.kategorie(), b.name()), b);
        }
        return n -> Optional.ofNullable(map.get(n));
    }
}
