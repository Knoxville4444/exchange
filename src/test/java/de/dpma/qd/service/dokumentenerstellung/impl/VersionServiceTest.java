package de.dpma.qd.service.dokumentenerstellung.impl;

import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.baustein;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.bausteinVersion;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.briefkopf;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.formular;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.formularVersion;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.tage;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.v;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.veroeffentlicht;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltDatenRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest implements WithAssertions {

    @Mock
    private VorlageRepository vorlageRepository;
    @Mock
    private InhaltRepository inhaltRepository;
    @Mock
    private InhaltDatenRepository inhaltDatenRepository;

    private final TestDatenbank db = new TestDatenbank();
    private VersionService service;

    @BeforeEach
    void setUp() {
        db.verdrahte(vorlageRepository, inhaltRepository, inhaltDatenRepository);
        AuditErzeuger audit = new AuditErzeuger(Clock.fixed(T0, ZoneOffset.UTC));
        BausteinService bausteine = new BausteinService(vorlageRepository, audit);
        InhaltService inhalte = new InhaltService(inhaltRepository, inhaltDatenRepository, bausteine, audit);
        service = new VersionService(vorlageRepository, inhaltRepository, inhalte, bausteine, audit);
    }

    @Nested
    class LegeAn {

        @Test
        void nebenversionOhneUploadKopiertInhaltSchemaUndBausteine() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0")));
            db.lege(formular("Bescheid", formularVersion("1.0", briefkopf("1"))));
            Version basis = db.vorlage("PATGBM", "Bescheid").version(v("1.0"));
            int inhalteVorher = db.anzahlInhalte();

            VersionsAnsicht neu = service.legeAn("PATGBM", "Bescheid", new VersionService.Antrag(VersionArt.NEBEN, null, null, "Kopie", null, null));

            assertThat(neu.version().nummer()).isEqualTo(v("1.1"));
            assertThat(neu.version().inhalt()).isEqualTo(basis.inhalt());
            assertThat(neu.version().schema()).isEqualTo(basis.schema());
            assertThat(neu.version().bausteine()).containsExactly(briefkopf("1"));
            assertThat(db.anzahlInhalte()).isEqualTo(inhalteVorher);
        }

        @Test
        void hauptversionZaehltHochUndNimmtNeuesSchema() {
            db.lege(formular("Bescheid", formularVersion("1.0"), formularVersion("1.1")));

            VersionsAnsicht neu = service.legeAn("PATGBM", "Bescheid",
                    new VersionService.Antrag(VersionArt.HAUPT, null, null, "Neues Feld", null, TestDateien.xsd("neu")));

            assertThat(neu.version().nummer()).isEqualTo(v("2.0"));
            assertThat(neu.version().schema()).isNotEqualTo(db.vorlage("PATGBM", "Bescheid").version(v("1.1")).schema());
        }

        @Test
        void nebenversionEinerAelterenHauptversionIstMoeglich() {
            db.lege(formular("Bescheid", formularVersion("1.0"), formularVersion("2.0")));

            VersionsAnsicht neu = service.legeAn("PATGBM", "Bescheid", new VersionService.Antrag(VersionArt.NEBEN, 1, null, null, null, null));

            assertThat(neu.version().nummer()).isEqualTo(v("1.1"));
            assertThat(neu.version().schema()).isEqualTo(db.vorlage("PATGBM", "Bescheid").version(v("1.0")).schema());
        }

        @Test
        void nebenversionDarfKeinEigenesSchemaHaben() {
            db.lege(formular("Bescheid", formularVersion("1.0")));

            assertThatThrownBy(() -> service.legeAn("PATGBM", "Bescheid",
                    new VersionService.Antrag(VersionArt.NEBEN, null, null, null, null, TestDateien.xsd("neu"))))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("SCHEMA_NICHT_ZULAESSIG"));
        }

        @Test
        void unbekannteHauptversionWirdGemeldet() {
            db.lege(formular("Bescheid", formularVersion("1.0")));

            assertThatThrownBy(() -> service.legeAn("PATGBM", "Bescheid", new VersionService.Antrag(VersionArt.NEBEN, 7, null, null, null, null)))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("HAUPTVERSION_NICHT_GEFUNDEN"));
        }

        @Test
        void neuerInhaltWirdMitDenBereichenAbgeglichen() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0"), bausteinVersion("2.0")));
            db.lege(formular("Bescheid", formularVersion("1.0")));

            VersionsAnsicht neu = service.legeAn("PATGBM", "Bescheid", new VersionService.Antrag(VersionArt.NEBEN, null, null, null,
                    TestDateien.odt("Bescheid", "baustein:PATGBM/Briefkopf"), null));

            assertThat(neu.version().bausteine()).containsExactly(briefkopf("2"));
        }
    }

    @Nested
    class Loesche {

        @Test
        void freigegebeneVersionIstNichtLoeschbar() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(1), null)));

            assertThatThrownBy(() -> service.loesche("PATGBM", "Bescheid", v("1.0")))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("VERSION_NICHT_DRAFT"));
        }

        @Test
        void letzteReferenzierteBausteinversionIstNichtLoeschbar() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0"), bausteinVersion("2.0")));
            db.lege(formular("Bescheid", formularVersion("1.0", briefkopf("2"))));

            assertThatThrownBy(() -> service.loesche("PATGBM", "Briefkopf", v("2.0")))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("BAUSTEIN_WIRD_VERWENDET"));
        }

        @Test
        void entwurfWirdEntfernt() {
            db.lege(formular("Bescheid", formularVersion("1.0"), formularVersion("1.1")));

            service.loesche("PATGBM", "Bescheid", v("1.1"));

            assertThat(db.vorlage("PATGBM", "Bescheid").versionen()).extracting(Version::nummer).containsExactly(v("1.0"));
        }
    }

    @Nested
    class ErsetzeSchema {

        @Test
        void giltFuerAlleVersionenDerHauptversion() {
            db.lege(formular("Bescheid", formularVersion("1.0"), formularVersion("1.1")));

            service.ersetzeSchema("PATGBM", "Bescheid", v("1.0"), TestDateien.xsd("neu"));

            Vorlage vorlage = db.vorlage("PATGBM", "Bescheid");
            assertThat(vorlage.version(v("1.1")).schema()).isEqualTo(vorlage.version(v("1.0")).schema());
        }

        @Test
        void nurAnDerHauptversion() {
            db.lege(formular("Bescheid", formularVersion("1.0"), formularVersion("1.1")));

            assertThatThrownBy(() -> service.ersetzeSchema("PATGBM", "Bescheid", v("1.1"), TestDateien.xsd("neu")))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("SCHEMA_NUR_HAUPTVERSION"));
        }

        @Test
        void unveraenderlichSobaldEineVersionFreigegebenIst() {
            db.lege(formular("Bescheid", formularVersion("1.0"), veroeffentlicht(formularVersion("1.1"), tage(1), null)));

            assertThatThrownBy(() -> service.ersetzeSchema("PATGBM", "Bescheid", v("1.0"), TestDateien.xsd("neu")))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("SCHEMA_IN_VERWENDUNG"));
        }
    }

    @Nested
    class SetzeBausteine {

        @Test
        void exaktesPinningWirdUebernommen() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0")));
            Version version = formularVersion("1.0", briefkopf("1"));
            db.lege(formular("Bescheid", version));
            db.registriere(version.inhalt(), TestDateien.odt("Bescheid", "baustein:PATGBM/Briefkopf").daten());

            VersionsAnsicht ansicht = service.setzeBausteine("PATGBM", "Bescheid", v("1.0"), List.of(briefkopf("1.0")));

            assertThat(ansicht.version().bausteine()).containsExactly(briefkopf("1.0"));
        }

        @Test
        void referenzenMuessenZuDenBereichenPassen() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0")));
            Version version = formularVersion("1.0", briefkopf("1"));
            db.lege(formular("Bescheid", version));
            db.registriere(version.inhalt(), TestDateien.odt("Bescheid", "baustein:PATGBM/Briefkopf").daten());

            assertThatThrownBy(() -> service.setzeBausteine("PATGBM", "Bescheid", v("1.0"), List.of()))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("BAUSTEINE_PASSEN_NICHT"));
        }
    }
}
