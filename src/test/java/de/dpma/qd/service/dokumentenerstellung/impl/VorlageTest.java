package de.dpma.qd.service.dokumentenerstellung.impl;

import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.angabe;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.audit;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.formular;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.formularVersion;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.tage;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.v;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.veroeffentlicht;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VorlageTest implements WithAssertions {

    /** 1.0, 1.1 und 2.0 veröffentlicht seit T0, 2.1 Entwurf, 2.2 geplant ab Tag 10. */
    private final Vorlage formular = formular("Bescheid",
            veroeffentlicht(formularVersion("1.0"), T0, null),
            veroeffentlicht(formularVersion("1.1"), T0, tage(3)),
            veroeffentlicht(formularVersion("2.0"), T0, null),
            formularVersion("2.1"),
            veroeffentlicht(formularVersion("2.2"), tage(10), null));

    @Nested
    class Loese {

        @Test
        void ohneAngabeHoechsteVeroeffentlichteVersion() {
            assertThat(formular.loese(null, T0).map(Version::nummer)).contains(v("2.0"));
        }

        @Test
        void hauptversionLiefertNeuesteVeroeffentlichteNebenversion() {
            assertThat(formular.loese(angabe("1"), T0).map(Version::nummer)).contains(v("1.1"));
        }

        @Test
        void entwurfWirdNichtAufgeloest() {
            assertThat(formular.loese(angabe("2.1"), T0)).isEmpty();
        }

        @Test
        void geplanteVersionGreiftZumTermin() {
            assertThat(formular.loese(angabe("2"), tage(9)).map(Version::nummer)).contains(v("2.0"));
            assertThat(formular.loese(angabe("2"), tage(10)).map(Version::nummer)).contains(v("2.2"));
        }

        @Test
        void zurueckgezogeneVersionFaelltAufDieVorherigeZurueck() {
            assertThat(formular.loese(angabe("1"), tage(3)).map(Version::nummer)).contains(v("1.0"));
        }

        @Test
        void vorlagenZeitraumSchraenktAlleVersionenEin() {
            Vorlage befristet = formular.mitZeitraum(new Zeitraum(null, tage(7)), audit());
            assertThat(befristet.loese(null, tage(6))).isPresent();
            assertThat(befristet.loese(null, tage(7))).isEmpty();
        }
    }

    @Nested
    class EffektiverStatus {

        @Test
        void nurEntwuerfeErgebenEntwurf() {
            assertThat(formular("Neu", formularVersion("1.0")).effektiverStatus(T0)).isEqualTo(EffektiverVorlagenStatus.ENTWURF);
        }

        @Test
        void geplanteVersionErgibtGeplant() {
            Vorlage geplant = formular("Neu", veroeffentlicht(formularVersion("1.0"), tage(5), null));
            assertThat(geplant.effektiverStatus(T0)).isEqualTo(EffektiverVorlagenStatus.GEPLANT);
            assertThat(geplant.effektiverStatus(tage(5))).isEqualTo(EffektiverVorlagenStatus.VEROEFFENTLICHT);
        }

        @Test
        void beendeterVorlagenZeitraumErgibtZurueckgezogen() {
            Vorlage befristet = formular.mitZeitraum(new Zeitraum(null, tage(7)), audit());
            assertThat(befristet.effektiverStatus(tage(7))).isEqualTo(EffektiverVorlagenStatus.ZURUECKGEZOGEN);
        }
    }

    @Nested
    class MitVersion {

        @Test
        void ersetztVersionMitGleicherNummerUndSortiertAbsteigend() {
            Vorlage neu = formular.mitVersion(veroeffentlicht(formularVersion("2.1"), tage(1), null));
            assertThat(neu.versionen()).extracting(Version::nummer).containsExactly(v("2.2"), v("2.1"), v("2.0"), v("1.1"), v("1.0"));
            assertThat(neu.version(v("2.1")).istDraft()).isFalse();
        }

        @Test
        void unbekannteVersionWirdGemeldet() {
            assertThatThrownBy(() -> formular.version(v("9.9"))).isInstanceOfSatisfying(FachlicherFehler.class,
                    f -> assertThat(f.art()).isEqualTo(FachlicherFehler.Art.NICHT_GEFUNDEN));
        }
    }
}
