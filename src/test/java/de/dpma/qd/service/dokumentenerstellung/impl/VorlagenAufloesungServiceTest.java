package de.dpma.qd.service.dokumentenerstellung.impl;

import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.angabe;
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
class VorlagenAufloesungServiceTest implements WithAssertions {

    @Mock
    private VorlageRepository vorlageRepository;
    @Mock
    private InhaltRepository inhaltRepository;
    @Mock
    private InhaltDatenRepository inhaltDatenRepository;

    private final TestDatenbank db = new TestDatenbank();
    private VorlagenAufloesungService service;

    @BeforeEach
    void setUp() {
        db.verdrahte(vorlageRepository, inhaltRepository, inhaltDatenRepository);
        AuditErzeuger audit = new AuditErzeuger(Clock.fixed(T0, ZoneOffset.UTC));
        BausteinService bausteine = new BausteinService(vorlageRepository, audit);
        service = new VorlagenAufloesungService(vorlageRepository, new InhaltService(inhaltRepository, inhaltDatenRepository, bausteine, audit),
                bausteine, audit);

        db.lege(baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), tage(-10), null),
                veroeffentlicht(bausteinVersion("1.1"), tage(30), null), bausteinVersion("1.2")));
        db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(-10), null)));
    }

    @Nested
    class Loese {

        @Test
        void liefertFormularMitAufgeloestemBaustein() {
            AufgeloesteVorlage vorlage = service.loese("PATGBM", "Bescheid", angabe("1"), null, VorlagenTyp.FORMULAR);

            assertThat(vorlage.version()).isEqualTo(v("1.0"));
            assertThat(vorlage.bausteinHeader()).isEqualTo("PATGBM/Briefkopf=1.0");
            assertThat(vorlage.bausteine()).singleElement().satisfies((AufgeloesteVorlage.Baustein b) -> {
                assertThat(b.bereichsname()).isEqualTo("baustein:PATGBM/Briefkopf");
                assertThat(new String(b.inhalt())).isEqualTo("Briefkopf 1.0");
            });
            assertThat(vorlage.schema()).isNotEmpty();
        }

        @Test
        void neueBausteinversionGreiftZumTermin() {
            assertThat(service.loese("PATGBM", "Bescheid", null, tage(30), VorlagenTyp.FORMULAR).bausteinHeader())
                    .isEqualTo("PATGBM/Briefkopf=1.1");
        }

        @Test
        void falscherTypWirdAbgelehnt() {
            assertThatThrownBy(() -> service.loese("PATGBM", "Bescheid", null, null, VorlagenTyp.MERKBLATT))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("TYP_UNPASSEND"));
        }

        @Test
        void unbekannteHauptversionWirdGemeldet() {
            assertThatThrownBy(() -> service.loese("PATGBM", "Bescheid", angabe("2"), null, VorlagenTyp.FORMULAR))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("KEINE_VERSION"));
        }
    }

    @Nested
    class LoeseFuerVorschau {

        @Test
        void ohneEntwuerfeGiltDieVeroeffentlichteBausteinversion() {
            assertThat(service.loeseFuerVorschau("PATGBM", "Bescheid", v("1.0"), false, null).bausteinHeader()).isEqualTo("PATGBM/Briefkopf=1.0");
        }

        @Test
        void mitEntwuerfenGiltDieHoechsteBausteinversion() {
            assertThat(service.loeseFuerVorschau("PATGBM", "Bescheid", v("1.0"), true, null).bausteinHeader()).isEqualTo("PATGBM/Briefkopf=1.2");
        }
    }

    @Nested
    class Ermittle {

        @Test
        void liefertVersionenOhneInhalte() {
            VorlagenAufloesungService.Ergebnis ergebnis = service.ermittle("PATGBM", "Bescheid", null, null);

            assertThat(ergebnis.version().nummer()).isEqualTo(v("1.0"));
            assertThat(ergebnis.bausteine()).singleElement().extracting(AufgeloesteReferenz::aufgeloesteVersion).isEqualTo(java.util.Optional.of(v("1.0")));
        }
    }
}
