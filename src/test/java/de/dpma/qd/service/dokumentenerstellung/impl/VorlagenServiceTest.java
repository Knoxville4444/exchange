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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltDatenRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

@ExtendWith(MockitoExtension.class)
class VorlagenServiceTest implements WithAssertions {

    @Mock
    private VorlageRepository vorlageRepository;
    @Mock
    private KategorieRepository kategorieRepository;
    @Mock
    private InhaltRepository inhaltRepository;
    @Mock
    private InhaltDatenRepository inhaltDatenRepository;

    private final TestDatenbank db = new TestDatenbank();
    private VorlagenService service;

    @BeforeEach
    void setUp() {
        db.verdrahte(vorlageRepository, inhaltRepository, inhaltDatenRepository);
        AuditErzeuger audit = new AuditErzeuger(Clock.fixed(T0, ZoneOffset.UTC));
        BausteinService bausteine = new BausteinService(vorlageRepository, audit);
        InhaltService inhalte = new InhaltService(inhaltRepository, inhaltDatenRepository, bausteine, audit);
        service = new VorlagenService(vorlageRepository, kategorieRepository, inhaltRepository, inhalte, bausteine, audit);
    }

    private void kategoriePatgbmExistiert() {
        KategorieEntity kategorie = new KategorieEntity();
        kategorie.setName("PATGBM");
        when(kategorieRepository.findByName("PATGBM")).thenReturn(Optional.of(kategorie));
    }

    @Nested
    class LegeAn {

        @Test
        void formularErkenntBausteinBereicheUndReferenziertHoechsteHauptversion() {
            kategoriePatgbmExistiert();
            db.lege(baustein("Briefkopf", bausteinVersion("1.0")));

            VorlagenAnsicht ansicht = service.legeAn("PATGBM", new VorlagenService.Antrag("Bescheid", VorlagenTyp.FORMULAR, "Bescheid", null,
                    Set.of("bescheid"), null, TestDateien.odt("Bescheid", "baustein:PATGBM/Briefkopf"), TestDateien.xsd()));

            assertThat(ansicht.effektiverStatus()).isEqualTo(EffektiverVorlagenStatus.ENTWURF);
            assertThat(ansicht.vorlage().version(v("1.0")).bausteine()).containsExactly(briefkopf("1"));
        }

        @Test
        void unbekannterBausteinImOdtWirdAbgelehnt() {
            kategoriePatgbmExistiert();

            assertThatThrownBy(() -> service.legeAn("PATGBM", new VorlagenService.Antrag("Bescheid", VorlagenTyp.FORMULAR, "Bescheid", null,
                    Set.of(), null, TestDateien.odt("Bescheid", "baustein:PATGBM/Briefkopf"), TestDateien.xsd())))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("BAUSTEIN_UNBEKANNT"));
        }

        @Test
        void merkblattOhneSchema() {
            kategoriePatgbmExistiert();

            assertThatThrownBy(() -> service.legeAn("PATGBM", new VorlagenService.Antrag("Gebuehren", VorlagenTyp.MERKBLATT, "Gebühren", null,
                    Set.of(), null, TestDateien.pdf(), TestDateien.xsd())))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("SCHEMA_NICHT_ZULAESSIG"));
        }

        @Test
        void unbekannteKategorieWirdGemeldet() {
            when(kategorieRepository.findByName("XYZ")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.legeAn("XYZ", new VorlagenService.Antrag("Gebuehren", VorlagenTyp.MERKBLATT, "Gebühren", null,
                    Set.of(), null, TestDateien.pdf(), null)))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.art()).isEqualTo(FachlicherFehler.Art.NICHT_GEFUNDEN));
        }
    }

    @Nested
    class Loesche {

        @Test
        void vorlageMitFreigegebenerVersionIstNichtLoeschbar() {
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0"), tage(1), null)));

            assertThatThrownBy(() -> service.loesche("PATGBM", "Bescheid"))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("VORLAGE_NICHT_LOESCHBAR"));
        }

        @Test
        void referenzierterBausteinIstNichtLoeschbar() {
            db.lege(baustein("Briefkopf", bausteinVersion("1.0")));
            db.lege(formular("Bescheid", formularVersion("1.0", briefkopf("1"))));

            assertThatThrownBy(() -> service.loesche("PATGBM", "Briefkopf"))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("BAUSTEIN_WIRD_VERWENDET"));
        }

        @Test
        void entwurfWirdGeloescht() {
            VorlageEntity entity = db.lege(formular("Bescheid", formularVersion("1.0")));

            service.loesche("PATGBM", "Bescheid");

            verify(vorlageRepository).delete(entity);
        }
    }

    @Nested
    class Verwendungen {

        @Test
        void zeigtReferenzierendeFormulareMitAufgeloesterVersion() {
            db.lege(baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), tage(-1), null)));
            db.lege(formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(-1), null), formularVersion("1.1",
                    briefkopf("1"))));

            List<Verwendung> verwendungen = service.verwendungen("PATGBM", "Briefkopf", null, true);

            assertThat(verwendungen).singleElement().satisfies((Verwendung vw) -> {
                assertThat(vw.formular()).isEqualTo("PATGBM/Bescheid");
                assertThat(vw.aufgeloesteVersion()).contains(v("1.0"));
            });
        }
    }
}
