package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.angabe;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.audit;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.formular;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.haupt;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.neben;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.tage;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.v;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.xsd;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VorlageTest {

    // --- Nummernvergabe -------------------------------------------------------------------

    @Test
    void neueVorlageHatVersion1_0AlsEntwurf() {
        Vorlage f = formular("Bescheid");
        assertEquals(List.of(v("1.0")), f.versionen().stream().map(Version::nummer).toList());
        assertEquals(EffektiverVersionStatus.DRAFT, f.version(v("1.0")).effektiverStatus(T0));
        assertEquals(EffektiverVorlagenStatus.ENTWURF, f.effektiverStatus(T0));
    }

    @Test
    void hauptversionZaehltDieHoechsteHauptversionHoch() {
        Vorlage f = formular("Bescheid");
        assertEquals(v("2.0"), haupt(f));
        assertEquals(v("3.0"), haupt(f));
    }

    @Test
    void nebenversionZaehltStandardmaessigInDerHoechstenHauptversion() {
        Vorlage f = formular("Bescheid");
        neben(f);                       // 1.1
        haupt(f);                       // 2.0
        assertEquals(v("2.1"), neben(f));
    }

    @Test
    void nebenversionEinerAelterenHauptversionIstMoeglich() {
        Vorlage f = formular("Bescheid");
        neben(f);                       // 1.1
        haupt(f);                       // 2.0
        Vorlage.VersionsPlan plan = f.planeNeueVersion(VersionArt.NEBEN, 1, null);
        assertEquals(v("1.2"), plan.nummer());
        assertEquals(Optional.of(v("1.1")), plan.basis().map(Version::nummer));
    }

    @Test
    void nebenversionEinerUnbekanntenHauptversionWirdAbgelehnt() {
        Vorlage f = formular("Bescheid");
        assertThrows(NichtGefundenException.class, () -> f.planeNeueVersion(VersionArt.NEBEN, 7, null));
    }

    @Test
    void basisversionMussZurHauptversionPassen() {
        Vorlage f = formular("Bescheid");
        haupt(f);                       // 2.0
        assertThrows(UngueltigeAnfrageException.class, () -> f.planeNeueVersion(VersionArt.NEBEN, 2, v("1.0")));
    }

    @Test
    void nebenversionUebernimmtDasSchemaDerHauptversion() {
        Vorlage f = formular("Bescheid");
        Version eins = f.version(v("1.0"));
        assertThrows(UngueltigeAnfrageException.class,
                () -> f.fuegeVersionHinzu(v("1.1"), null, eins.inhalt(), xsd(), List.of(), audit()));
    }

    // --- Veröffentlichung und Status --------------------------------------------------------

    @Test
    void sofortVeroeffentlichen() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        assertEquals(EffektiverVersionStatus.VEROEFFENTLICHT, f.version(v("1.0")).effektiverStatus(T0));
        assertEquals(EffektiverVorlagenStatus.VEROEFFENTLICHT, f.effektiverStatus(T0));
    }

    @Test
    void geplanteVeroeffentlichungGreiftZumTermin() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), tage(30), null, T0, audit());
        Version eins = f.version(v("1.0"));
        assertEquals(EffektiverVersionStatus.GEPLANT, eins.effektiverStatus(tage(29)));
        assertEquals(EffektiverVersionStatus.VEROEFFENTLICHT, eins.effektiverStatus(tage(30)));
        assertEquals(EffektiverVorlagenStatus.GEPLANT, f.effektiverStatus(T0));
    }

    @Test
    void geplanterTerminIstVerschiebbarUndAbsagbar() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), tage(30), null, T0, audit());
        f.setzeVeroeffentlichung(v("1.0"), tage(40), null, T0, audit());
        assertEquals(tage(40), f.version(v("1.0")).zeitraum().ab());
        f.sageVeroeffentlichungAb(v("1.0"), T0, audit());
        assertEquals(EffektiverVersionStatus.DRAFT, f.version(v("1.0")).effektiverStatus(T0));
    }

    @Test
    void abInDerVergangenheitIstNichtZulaessig() {
        Vorlage f = formular("Bescheid");
        assertThrows(UngueltigeAnfrageException.class, () -> f.setzeVeroeffentlichung(v("1.0"), tage(-1), null, T0, audit()));
    }

    @Test
    void beiVeroeffentlichtenVersionenIstNurNochBisAenderbar() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        assertThrows(KonfliktException.class, () -> f.setzeVeroeffentlichung(v("1.0"), tage(5), null, tage(1), audit()));
        f.setzeVeroeffentlichung(v("1.0"), null, tage(10), tage(1), audit());
        assertEquals(EffektiverVersionStatus.ZURUECKGEZOGEN, f.version(v("1.0")).effektiverStatus(tage(10)));
    }

    @Test
    void zurueckgezogeneVersionenSindUnveraenderlich() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), null, tage(1), T0, audit());
        assertThrows(KonfliktException.class, () -> f.setzeVeroeffentlichung(v("1.0"), null, null, tage(2), audit()));
    }

    @Test
    void nurGeplanteVeroeffentlichungenKoennenAbgesagtWerden() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        assertThrows(KonfliktException.class, () -> f.sageVeroeffentlichungAb(v("1.0"), T0, audit()));
    }

    @Test
    void freigegebeneVersionenSindInhaltlichUnveraenderlich() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), tage(5), null, T0, audit());
        assertThrows(KonfliktException.class, () -> f.ersetzeInhalt(v("1.0"), Testdaten.odt(), List.of(), audit()));
        assertThrows(KonfliktException.class, () -> f.loescheVersion(v("1.0")));
        assertFalse(f.istLoeschbar());
    }

    @Test
    void schemaIstNurAnDerHauptversionUndVorDerFreigabeAenderbar() {
        Vorlage f = formular("Bescheid");
        neben(f);                       // 1.1
        assertThrows(KonfliktException.class, () -> f.ersetzeSchema(v("1.1"), xsd(), audit()));
        InhaltRef neu = xsd();
        f.ersetzeSchema(v("1.0"), neu, audit());
        assertEquals(neu, f.version(v("1.1")).schema(), "Schema gilt für die ganze Hauptversion");
        f.setzeVeroeffentlichung(v("1.1"), null, null, T0, audit());
        assertThrows(KonfliktException.class, () -> f.ersetzeSchema(v("1.0"), xsd(), audit()));
    }

    // --- Versionsauflösung -----------------------------------------------------------------

    @Test
    void hauptversionLiefertDieNeuesteVeroeffentlichteNebenversion() {
        Vorlage f = vorlageMit_1_0_1_1_2_0_veroeffentlicht();
        assertEquals(v("2.0"), f.loese(null, T0).orElseThrow().nummer());
        assertEquals(v("1.1"), f.loese(angabe("1"), T0).orElseThrow().nummer());
        assertEquals(v("1.0"), f.loese(angabe("1.0"), T0).orElseThrow().nummer());
    }

    @Test
    void entwuerfeUndGeplanteVersionenWerdenNichtAufgeloest() {
        Vorlage f = vorlageMit_1_0_1_1_2_0_veroeffentlicht();
        VersionsNummer entwurf = neben(f);              // 2.1 (DRAFT)
        VersionsNummer geplant = neben(f);              // 2.2
        f.setzeVeroeffentlichung(geplant, tage(10), null, T0, audit());
        assertEquals(v("2.0"), f.loese(angabe("2"), T0).orElseThrow().nummer());
        assertTrue(f.loese(VersionsAngabe.exakt(entwurf), T0).isEmpty());
        assertEquals(v("2.2"), f.loese(angabe("2"), tage(10)).orElseThrow().nummer(), "zum Termin automatisch");
    }

    @Test
    void zurueckgezogeneVersionFaelltAufDieVorherigeZurueck() {
        Vorlage f = vorlageMit_1_0_1_1_2_0_veroeffentlicht();
        f.setzeVeroeffentlichung(v("1.1"), null, tage(3), T0, audit());
        assertEquals(v("1.1"), f.loese(angabe("1"), tage(2)).orElseThrow().nummer());
        assertEquals(v("1.0"), f.loese(angabe("1"), tage(3)).orElseThrow().nummer());
    }

    @Test
    void stichtagLiefertDenFrueherenStand() {
        Vorlage f = formular("Bescheid");
        f.setzeVeroeffentlichung(v("1.0"), null, null, T0, audit());
        VersionsNummer neu = neben(f);
        f.setzeVeroeffentlichung(neu, tage(30), null, T0, audit());
        assertEquals(v("1.0"), f.loese(angabe("1"), tage(29)).orElseThrow().nummer());
        assertEquals(v("1.1"), f.loese(angabe("1"), tage(31)).orElseThrow().nummer());
    }

    @Test
    void vorlagenZeitraumSchraenktAlleVersionenEin() {
        Vorlage f = vorlageMit_1_0_1_1_2_0_veroeffentlicht();
        f.setzeVorlagenZeitraum(new Zeitraum(null, tage(7)), T0, audit());
        assertTrue(f.loese(null, tage(6)).isPresent());
        assertTrue(f.loese(null, tage(7)).isEmpty());
        assertEquals(EffektiverVorlagenStatus.ZURUECKGEZOGEN, f.effektiverStatus(tage(7)));
    }

    @Test
    void vorlagenZeitraumDarfDieVergangenheitNichtAendern() {
        Vorlage f = vorlageMit_1_0_1_1_2_0_veroeffentlicht();
        assertThrows(KonfliktException.class,
                () -> f.setzeVorlagenZeitraum(new Zeitraum(tage(-1), null), T0, audit()));
        f.setzeVorlagenZeitraum(new Zeitraum(null, tage(7)), T0, audit());
        assertThrows(KonfliktException.class, () -> f.setzeVorlagenZeitraum(null, tage(8), audit()));
    }

    @Test
    void stammdatenSindAenderbar() {
        Vorlage f = formular("Bescheid");
        f.aendereStammdaten("Neue Bezeichnung", "Text", Set.of("bescheid"), audit());
        assertEquals("Neue Bezeichnung", f.bezeichnung());
        assertThrows(UngueltigeAnfrageException.class, () -> f.aendereStammdaten(" ", null, Set.of(), audit()));
    }

    private static Vorlage vorlageMit_1_0_1_1_2_0_veroeffentlicht() {
        Vorlage f = formular("Bescheid");
        neben(f);
        haupt(f);
        for (String nummer : List.of("1.0", "1.1", "2.0")) {
            f.setzeVeroeffentlichung(v(nummer), null, null, T0, audit());
        }
        return f;
    }
}
