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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

@ExtendWith(MockitoExtension.class)
class VeroeffentlichungServiceTest implements WithAssertions {

    @Mock
    private VorlageRepository vorlageRepository;
    @Mock
    private InhaltRepository inhaltRepository;
    @Mock
    private InhaltDatenRepository inhaltDatenRepository;

    private final TestDatenbank db = new TestDatenbank();
    private VeroeffentlichungService service;

    @BeforeEach
    void setUp() {
        db.verdrahte(vorlageRepository, inhaltRepository, inhaltDatenRepository);
        AuditErzeuger audit = new AuditErzeuger(Clock.fixed(T0, ZoneOffset.UTC));
        service = new VeroeffentlichungService(vorlageRepository, inhaltRepository, new BausteinService(vorlageRepository, audit), audit);
    }

    @Nested
    class SetzeVersion {

        @Test
        void ohneAbWirdSofortVeroeffentlicht() {
            db.lege(formular("Bescheid", formularVersion("1.0")));

            VersionsAnsicht ansicht = service.setzeVersion("PATGBM", "Bescheid", v("1.0"), null, null);

            assertThat(ansicht.effektiverStatus()).isEqualTo(EffektiverVersionStatus.VEROEFFENTLICHT);
            assertThat(db.vorlage("PATGBM", "Bescheid").version(v("1.0")).zeitraum().ab()).isEqualTo(T0);
        }

        @Test
        void geplanterTerminIstVerschiebbar() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(30), null)));

            VersionsAnsicht ansicht = service.setzeVersion("PATGBM", "Bescheid", v("1.0"), tage(40), null);

            assertThat(ansicht.effektiverStatus()).isEqualTo(EffektiverVersionStatus.GEPLANT);
            assertThat(ansicht.version().zeitraum().ab()).isEqualTo(tage(40));
        }

        @Test
        void abInDerVergangenheitIstNichtZulaessig() {
            db.lege(formular("Bescheid", formularVersion("1.0")));

            assertThatThrownBy(() -> service.setzeVersion("PATGBM", "Bescheid", v("1.0"), tage(-1), null))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("AB_IN_VERGANGENHEIT"));
        }

        @Test
        void beiVeroeffentlichtenVersionenIstNurNochBisAenderbar() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(-10), null)));

            assertThatThrownBy(() -> service.setzeVersion("PATGBM", "Bescheid", v("1.0"), tage(5), null))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("AB_UNVERAENDERLICH"));
            VersionsAnsicht ansicht = service.setzeVersion("PATGBM", "Bescheid", v("1.0"), null, tage(10));
            assertThat(ansicht.version().zeitraum().bis()).isEqualTo(tage(10));
        }

        @Test
        void zurueckgezogeneVersionenSindUnveraenderlich() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(-10), tage(-1))));

            assertThatThrownBy(() -> service.setzeVersion("PATGBM", "Bescheid", v("1.0"), null, null))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("VERSION_ZURUECKGEZOGEN"));
        }

        @Test
        void formularMitNurEntworfenemBausteinWirdNichtGespeichert() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0")));
            db.lege(formular("Bescheid", formularVersion("1.0", briefkopf("1"))));

            assertThatThrownBy(() -> service.setzeVersion("PATGBM", "Bescheid", v("1.0"), null, null))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("BAUSTEIN_NICHT_AUFLOESBAR"));
            verify(vorlageRepository, never()).saveAndFlush(any(VorlageEntity.class));
        }

        @Test
        void benoetigterBausteinKannNichtZurueckgezogenWerden() {
            db.lege(baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), tage(-10), null)));
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(-10), null)));

            assertThatThrownBy(() -> service.setzeVersion("PATGBM", "Briefkopf", v("1.0"), null, T0))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> {
                        assertThat(f.errorId()).isEqualTo("BAUSTEIN_WIRD_VERWENDET");
                        assertThat(f.detail().containsKey("verwendungen")).isTrue();
                    });
        }

        @Test
        void bausteinMitNachfolgerZumSelbenTerminDarfZurueckgezogenWerden() {
            db.lege(baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), tage(-10), null),
                    veroeffentlicht(bausteinVersion("1.1"), tage(5), null)));
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(-10), null)));

            VersionsAnsicht ansicht = service.setzeVersion("PATGBM", "Briefkopf", v("1.0"), null, tage(5));

            assertThat(ansicht.version().zeitraum().bis()).isEqualTo(tage(5));
        }
    }

    @Nested
    class SageVersionAb {

        @Test
        void geplanteVersionWirdWiederEntwurf() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(30), null)));

            VersionsAnsicht ansicht = service.sageVersionAb("PATGBM", "Bescheid", v("1.0"));

            assertThat(ansicht.effektiverStatus()).isEqualTo(EffektiverVersionStatus.DRAFT);
        }

        @Test
        void veroeffentlichteVersionKannNichtAbgesagtWerden() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(-1), null)));

            assertThatThrownBy(() -> service.sageVersionAb("PATGBM", "Bescheid", v("1.0")))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("VERSION_NICHT_GEPLANT"));
        }
    }

    @Nested
    class SetzeVorlage {

        @Test
        void befristungInDieZukunftIstErlaubt() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(-10), null)));

            VorlagenAnsicht ansicht = service.setzeVorlage("PATGBM", "Bescheid", new Zeitraum(null, tage(7)));

            assertThat(ansicht.vorlage().veroeffentlichung().bis()).isEqualTo(tage(7));
        }

        @Test
        void vergangenheitIstUnveraenderlich() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(-10), null)));

            assertThatThrownBy(() -> service.setzeVorlage("PATGBM", "Bescheid", new Zeitraum(tage(-1), null)))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("VERGANGENHEIT_UNVERAENDERLICH"));
        }
    }
}
