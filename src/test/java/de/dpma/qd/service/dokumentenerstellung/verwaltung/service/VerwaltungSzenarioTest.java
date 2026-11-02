package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVorlagenStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionArt;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Zeitraum;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.FachlicherFehler;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung.AufgeloesteVorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenFilter;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten.TestDateien;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten.Verwaltung;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Durchläufe über alle Services mit In-Memory-Adaptern. */
class VerwaltungSzenarioTest {

    private static final Instant START = Instant.parse("2026-12-01T10:00:00Z");
    private static final Instant SILVESTER = Instant.parse("2026-12-31T23:00:00Z");   // 0:00 Uhr MEZ
    private static final BausteinName BRIEFKOPF = new BausteinName("PATGBM", "Briefkopf");

    private Verwaltung verwaltung;

    @BeforeEach
    void setUp() {
        verwaltung = new Verwaltung(START);
        verwaltung.kategorien.speichere("PATGBM", "Patente und Gebrauchsmuster");
    }

    @Test
    void formularMitBausteinErzeugenUndBausteinZuSilvesterAustauschen() {
        legeBriefkopfAn("Briefkopf alt");
        veroeffentliche("Briefkopf", "1.0");
        legeBescheidAn();
        veroeffentliche("Bescheid", "1.0");

        VersionsAnsicht neuerKopf = verwaltung.versionen.legeAn("PATGBM", "Briefkopf",
                new VersionsAntrag(VersionArt.NEBEN, null, null, "Neues Logo", TestDateien.odt("Briefkopf neu"), null));
        verwaltung.versionen.setzeVeroeffentlichung("PATGBM", "Briefkopf", neuerKopf.version().nummer(), SILVESTER, null);

        AufgeloesteVorlage vorher = verwaltung.aufloesung.loese("PATGBM", "Bescheid", VersionsAngabe.parse("1"), null, VorlagenTyp.FORMULAR);
        assertEquals("PATGBM/Briefkopf=1.0", vorher.bausteinHeader());

        verwaltung.uhr.stelle(SILVESTER);
        AufgeloesteVorlage nachher = verwaltung.aufloesung.loese("PATGBM", "Bescheid", VersionsAngabe.parse("1"), null, VorlagenTyp.FORMULAR);
        assertEquals("PATGBM/Briefkopf=1.1", nachher.bausteinHeader());
        assertEquals("baustein:PATGBM/Briefkopf", nachher.bausteine().getFirst().bereichsname());
        assertArrayEquals(TestDateien.odt("Briefkopf neu").daten(), nachher.bausteine().getFirst().inhalt());

        AufgeloesteVorlage rekonstruiert = verwaltung.aufloesung.loese("PATGBM", "Bescheid", VersionsAngabe.parse("1"),
                START.plus(Duration.ofDays(1)), VorlagenTyp.FORMULAR);
        assertEquals("PATGBM/Briefkopf=1.0", rekonstruiert.bausteinHeader(), "Stichtag rekonstruiert den alten Stand");
    }

    @Test
    void bausteinAnlageErkenntBereicheUndSetztHoechsteHauptversion() {
        legeBriefkopfAn("Briefkopf");
        VorlagenAnsicht bescheid = legeBescheidAn();
        VersionsAnsicht v = verwaltung.versionen.lies("PATGBM", "Bescheid", VersionsNummer.erste(), null);
        assertEquals(List.of(new BausteinReferenz(BRIEFKOPF, VersionsAngabe.parse("1"))), v.version().bausteine());
        assertEquals(EffektiverVorlagenStatus.ENTWURF, bescheid.effektiverStatus());
    }

    @Test
    void unbekannterBausteinImOdtWirdAbgelehnt() {
        FachlicherFehler fehler = assertThrows(UngueltigeAnfrageException.class, this::legeBescheidAn);
        assertEquals("BAUSTEIN_UNBEKANNT", fehler.errorId());
    }

    @Test
    void formularMitUnveroeffentlichtemBausteinIstNichtVeroeffentlichbar() {
        legeBriefkopfAn("Briefkopf");
        legeBescheidAn();
        KonfliktException fehler = assertThrows(KonfliktException.class, () -> veroeffentliche("Bescheid", "1.0"));
        assertEquals("BAUSTEIN_NICHT_AUFLOESBAR", fehler.errorId());
        assertEquals(EffektiverVorlagenStatus.ENTWURF, verwaltung.vorlagen.lies("PATGBM", "Bescheid", null).effektiverStatus(),
                "nach dem Fehler bleibt alles beim Alten");
    }

    @Test
    void benoetigterBausteinKannNichtZurueckgezogenWerden() {
        legeBriefkopfAn("Briefkopf");
        veroeffentliche("Briefkopf", "1.0");
        legeBescheidAn();
        veroeffentliche("Bescheid", "1.0");
        verwaltung.uhr.vor(Duration.ofHours(1));
        Instant sofort = verwaltung.uhr.instant();

        KonfliktException fehler = assertThrows(KonfliktException.class,
                () -> verwaltung.versionen.setzeVeroeffentlichung("PATGBM", "Briefkopf", VersionsNummer.erste(), null, sofort));
        assertEquals("BAUSTEIN_WIRD_VERWENDET", fehler.errorId());
        assertTrue(fehler.detail().containsKey("verwendungen"));
        assertThrows(KonfliktException.class, () -> verwaltung.vorlagen.setzeZeitraum("PATGBM", "Briefkopf",
                new Zeitraum(null, START.plus(Duration.ofDays(1)))));
    }

    @Test
    void neueVersionOhneUploadKopiertInhaltUndKannGepinntWerden() {
        legeBriefkopfAn("Briefkopf");
        legeBescheidAn();
        int inhalteVorher = verwaltung.inhalte.anzahl();

        VersionsAnsicht kopie = verwaltung.versionen.legeAn("PATGBM", "Bescheid",
                new VersionsAntrag(VersionArt.NEBEN, null, null, "Pin", null, null));
        assertEquals(VersionsNummer.parse("1.1"), kopie.version().nummer());
        assertEquals(inhalteVorher, verwaltung.inhalte.anzahl(), "Kopie belegt keinen zusätzlichen Speicher");

        VersionsAnsicht gepinnt = verwaltung.versionen.setzeBausteine("PATGBM", "Bescheid", kopie.version().nummer(),
                List.of(new BausteinReferenz(BRIEFKOPF, VersionsAngabe.parse("1.0"))));
        assertTrue(gepinnt.version().bausteine().getFirst().angabe().istExakt());

        assertThrows(UngueltigeAnfrageException.class, () -> verwaltung.versionen.setzeBausteine("PATGBM", "Bescheid",
                kopie.version().nummer(), List.of()));
    }

    @Test
    void nebenversionDarfKeinEigenesSchemaHaben() {
        legeBriefkopfAn("Briefkopf");
        legeBescheidAn();
        assertThrows(UngueltigeAnfrageException.class, () -> verwaltung.versionen.legeAn("PATGBM", "Bescheid",
                new VersionsAntrag(VersionArt.NEBEN, null, null, null, null, TestDateien.xsd("neu"))));
        VersionsAnsicht zwei = verwaltung.versionen.legeAn("PATGBM", "Bescheid",
                new VersionsAntrag(VersionArt.HAUPT, null, null, "Neues Feld", null, TestDateien.xsd("neu")));
        assertEquals(VersionsNummer.parse("2.0"), zwei.version().nummer());
    }

    @Test
    void merkblattWirdUnveraendertAusgeliefert() {
        verwaltung.vorlagen.legeAn("PATGBM", new VorlagenAntrag("Gebuehren", VorlagenTyp.MERKBLATT, "Gebührenhinweise",
                null, Set.of("gebuehren"), null, TestDateien.pdf(), null));
        veroeffentliche("Gebuehren", "1.0");
        AufgeloesteVorlage merkblatt = verwaltung.aufloesung.loese("PATGBM", "Gebuehren", null, null, VorlagenTyp.MERKBLATT);
        assertArrayEquals(TestDateien.pdf().daten(), merkblatt.inhalt());
        assertThrows(KonfliktException.class,
                () -> verwaltung.aufloesung.loese("PATGBM", "Gebuehren", null, null, VorlagenTyp.FORMULAR));
        assertThrows(UngueltigeAnfrageException.class, () -> verwaltung.vorlagen.legeAn("PATGBM",
                new VorlagenAntrag("Falsch", VorlagenTyp.MERKBLATT, "x", null, null, null, TestDateien.pdf(), TestDateien.xsd())));
    }

    @Test
    void vorschauNutztAufWunschBausteinEntwuerfe() {
        legeBriefkopfAn("Briefkopf alt");
        veroeffentliche("Briefkopf", "1.0");
        legeBescheidAn();
        verwaltung.versionen.legeAn("PATGBM", "Briefkopf",
                new VersionsAntrag(VersionArt.NEBEN, null, null, null, TestDateien.odt("Entwurf"), null));

        assertEquals("PATGBM/Briefkopf=1.0", verwaltung.aufloesung
                .loeseFuerVorschau("PATGBM", "Bescheid", VersionsNummer.erste(), false, null).bausteinHeader());
        assertEquals("PATGBM/Briefkopf=1.1", verwaltung.aufloesung
                .loeseFuerVorschau("PATGBM", "Bescheid", VersionsNummer.erste(), true, null).bausteinHeader());
    }

    @Test
    void verwendungsnachweisZeigtReferenzierendeFormulare() {
        legeBriefkopfAn("Briefkopf");
        veroeffentliche("Briefkopf", "1.0");
        legeBescheidAn();
        veroeffentliche("Bescheid", "1.0");

        List<Verwendung> verwendungen = verwaltung.vorlagen.verwendungen("PATGBM", "Briefkopf", null, true);
        assertEquals(1, verwendungen.size());
        assertEquals("PATGBM/Bescheid", verwendungen.getFirst().formular());
        assertEquals(Optional.of(VersionsNummer.erste()), verwendungen.getFirst().aufgeloesteVersion());
    }

    @Test
    void loeschenIstNurFuerNieFreigegebeneVorlagenMoeglich() {
        legeBriefkopfAn("Briefkopf");
        legeBescheidAn();
        assertThrows(KonfliktException.class, () -> verwaltung.vorlagen.loesche("PATGBM", "Briefkopf"),
                "Baustein wird noch referenziert");
        verwaltung.vorlagen.loesche("PATGBM", "Bescheid");
        verwaltung.vorlagen.loesche("PATGBM", "Briefkopf");
        assertThrows(NichtGefundenException.class, () -> verwaltung.vorlagen.lies("PATGBM", "Briefkopf", null));
    }

    @Test
    void kategorieMitVorlagenIstNichtLoeschbar() {
        legeBriefkopfAn("Briefkopf");
        assertThrows(KonfliktException.class, () -> verwaltung.kategorien.loesche("PATGBM"));
        assertEquals(1L, verwaltung.kategorien.alle().getFirst().anzahlVorlagen());
    }

    @Test
    void sucheFiltertNachTypUndTag() {
        legeBriefkopfAn("Briefkopf");
        verwaltung.vorlagen.legeAn("PATGBM", new VorlagenAntrag("Gebuehren", VorlagenTyp.MERKBLATT, "Gebührenhinweise",
                null, Set.of("gebuehren"), null, TestDateien.pdf(), null));
        assertEquals(1, verwaltung.vorlagen.suche(new VorlagenFilter(null, Set.of(VorlagenTyp.BAUSTEIN), null, null), null).size());
        assertEquals(1, verwaltung.vorlagen.suche(new VorlagenFilter(null, null, Set.of("gebuehren"), null), null).size());
        assertEquals(2, verwaltung.vorlagen.suche(VorlagenFilter.alle(), null).size());
    }

    // -------------------------------------------------------------------------------------

    private void legeBriefkopfAn(String text) {
        verwaltung.vorlagen.legeAn("PATGBM", new VorlagenAntrag("Briefkopf", VorlagenTyp.BAUSTEIN, "Briefkopf",
                null, Set.of(), null, TestDateien.odt(text), null));
    }

    private VorlagenAnsicht legeBescheidAn() {
        return verwaltung.vorlagen.legeAn("PATGBM", new VorlagenAntrag("Bescheid", VorlagenTyp.FORMULAR, "Bescheid",
                null, Set.of("bescheid"), "erste Fassung", TestDateien.odt("Bescheid", "baustein:PATGBM/Briefkopf"),
                TestDateien.xsd()));
    }

    private void veroeffentliche(String name, String version) {
        verwaltung.versionen.setzeVeroeffentlichung("PATGBM", name, VersionsNummer.parse(version), null, null);
    }
}
