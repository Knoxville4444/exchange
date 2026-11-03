package de.dpma.qd.service.dokumentenerstellung.impl;

import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.BRIEFKOPF;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BausteinPruefungTest implements WithAssertions {

    private static Function<BausteinName, Optional<Vorlage>> quelle(Vorlage baustein) {
        return n -> n.equals(baustein.alsBausteinName()) ? Optional.of(baustein) : Optional.empty();
    }

    @Nested
    class PruefeFormularVersion {

        @Test
        void aufloesbarerBausteinIstInOrdnung() {
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), T0, null));
            Vorlage f = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), T0, null));
            assertThat(BausteinPruefung.pruefeFormularVersion(f, f.version(v("1.0")), quelle(b), T0)).isEmpty();
        }

        @Test
        void nurEntworfenerBausteinIstNichtAufloesbar() {
            Vorlage b = baustein("Briefkopf", bausteinVersion("1.0"));
            Vorlage f = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(10), null));
            assertThat(BausteinPruefung.pruefeFormularVersion(f, f.version(v("1.0")), quelle(b), T0))
                    .singleElement().extracting(BausteinPruefung.FehlendeAufloesung::zeitpunkt).isEqualTo(tage(10));
        }

        @Test
        void formularDarfErstAbDemBausteinterminGelten() {
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), tage(10), null));
            Vorlage zuFrueh = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(9), null));
            Vorlage passend = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), tage(10), null));
            assertThat(BausteinPruefung.pruefeFormularVersion(zuFrueh, zuFrueh.version(v("1.0")), quelle(b), T0)).hasSize(1);
            assertThat(BausteinPruefung.pruefeFormularVersion(passend, passend.version(v("1.0")), quelle(b), T0)).isEmpty();
        }
    }

    @Nested
    class PruefeBausteinAenderung {

        private final Vorlage formularMitHauptversion = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), T0, null));

        @Test
        void rueckzugMitLueckeWirdErkannt() {
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), T0, tage(15)),
                    veroeffentlicht(bausteinVersion("1.1"), tage(20), null));
            assertThat(BausteinPruefung.pruefeBausteinAenderung(b, List.of(formularMitHauptversion), T0))
                    .singleElement().extracting(BausteinPruefung.FehlendeAufloesung::zeitpunkt).isEqualTo(tage(15));
        }

        @Test
        void rueckzugGenauZumStartDesNachfolgersIstErlaubt() {
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), T0, tage(20)),
                    veroeffentlicht(bausteinVersion("1.1"), tage(20), null));
            assertThat(BausteinPruefung.pruefeBausteinAenderung(b, List.of(formularMitHauptversion), T0)).isEmpty();
        }

        @Test
        void exaktGepinnteVersionDarfNichtZurueckgezogenWerden() {
            Vorlage gepinnt = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1.0")), T0, null));
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), T0, tage(5)),
                    veroeffentlicht(bausteinVersion("1.1"), T0, null));
            assertThat(BausteinPruefung.pruefeBausteinAenderung(b, List.of(gepinnt), T0)).hasSize(1);
        }

        @Test
        void neueHauptversionIstKeinErsatz() {
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), T0, tage(1)),
                    veroeffentlicht(bausteinVersion("2.0"), T0, null));
            assertThat(BausteinPruefung.pruefeBausteinAenderung(b, List.of(formularMitHauptversion), T0)).hasSize(1);
        }

        @Test
        void zurueckgezogeneFormulareBrauchenKeineBausteine() {
            Vorlage altesFormular = formular("Bescheid", veroeffentlicht(formularVersion("1.0", briefkopf("1")), T0, tage(5)));
            Vorlage b = baustein("Briefkopf", veroeffentlicht(bausteinVersion("1.0"), T0, tage(5)));
            assertThat(BausteinPruefung.pruefeBausteinAenderung(b, List.of(altesFormular), T0)).isEmpty();
        }
    }

    @Nested
    class GleicheAb {

        @Test
        void behaeltBestehendeUndErgaenztNeueReferenzMitHoechsterHauptversion() {
            Vorlage briefkopf = baustein("Briefkopf", bausteinVersion("1.0"), bausteinVersion("2.0"));
            Vorlage fusszeile = baustein("Fusszeile", bausteinVersion("1.0"));
            BausteinName fuss = fusszeile.alsBausteinName();
            Function<BausteinName, Optional<Vorlage>> quelle = n -> Optional.ofNullable(
                    n.equals(BRIEFKOPF) ? briefkopf : n.equals(fuss) ? fusszeile : null);

            List<BausteinReferenz> ergebnis = BausteinPruefung.gleicheAb(Set.of(BRIEFKOPF, fuss),
                    List.of(briefkopf("1.0"), new BausteinReferenz(new BausteinName("PATGBM", "Entfallen"), angabe("1"))), quelle);

            assertThat(ergebnis).containsExactly(briefkopf("1.0"), new BausteinReferenz(fuss, angabe("1")));
        }

        @Test
        void unbekannterBausteinWirdAbgelehnt() {
            assertThatThrownBy(() -> BausteinPruefung.gleicheAb(Set.of(BRIEFKOPF), List.of(), n -> Optional.empty()))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("BAUSTEIN_UNBEKANNT"));
        }
    }

    @Nested
    class PruefeReferenzen {

        private final Vorlage b = baustein("Briefkopf", bausteinVersion("1.0"));

        @Test
        void referenzenMuessenZuDenBereichenPassen() {
            assertThatThrownBy(() -> BausteinPruefung.pruefeReferenzen(Set.of(), List.of(briefkopf("1")), quelle(b)))
                    .isInstanceOf(FachlicherFehler.class);
        }

        @Test
        void unbekannteVersionWirdAbgelehnt() {
            assertThatThrownBy(() -> BausteinPruefung.pruefeReferenzen(Set.of(BRIEFKOPF), List.of(briefkopf("1.5")), quelle(b)))
                    .isInstanceOf(FachlicherFehler.class);
        }

        @Test
        void passendeReferenzWirdUebernommen() {
            assertThat(BausteinPruefung.pruefeReferenzen(Set.of(BRIEFKOPF), List.of(briefkopf("1.0")), quelle(b))).hasSize(1);
        }
    }
}
