package de.dpma.qd.service.dokumentenerstellung.impl;

import java.util.List;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VersionsNummerTest implements WithAssertions {

    @Nested
    class Parse {

        @Test
        void liestHauptUndNebenversion() {
            assertThat(VersionsNummer.parse("2.13")).isEqualTo(new VersionsNummer(2, 13));
        }

        @Test
        void lehntUngueltigeFormateAb() {
            for (String ungueltig : List.of("", "1", "0.1", "1.", ".1", "1.2.3", "a.b", "-1.0")) {
                assertThatThrownBy(() -> VersionsNummer.parse(ungueltig)).isInstanceOf(FachlicherFehler.class);
            }
        }
    }

    @Nested
    class CompareTo {

        @Test
        void vergleichtNumerischStattAlsText() {
            assertThat(List.of("2.0", "1.10", "1.9").stream().map(VersionsNummer::parse).sorted().map(Object::toString).toList())
                    .containsExactly("1.9", "1.10", "2.0");
        }
    }

    @Nested
    class Angabe {

        @Test
        void hauptversionPasstAufAlleIhreNebenversionen() {
            VersionsAngabe zwei = VersionsAngabe.parse("2");
            assertThat(zwei.istExakt()).isFalse();
            assertThat(zwei.passtZu(VersionsNummer.parse("2.7"))).isTrue();
            assertThat(zwei.passtZu(VersionsNummer.parse("3.0"))).isFalse();
        }

        @Test
        void exakteAngabePasstNurAufDieseVersion() {
            VersionsAngabe exakt = VersionsAngabe.parse("2.3");
            assertThat(exakt.passtZu(VersionsNummer.parse("2.3"))).isTrue();
            assertThat(exakt.passtZu(VersionsNummer.parse("2.4"))).isFalse();
        }

        @Test
        void leereAngabeBedeutetKeineEinschraenkung() {
            assertThat(VersionsAngabe.parseOptional(" ")).isEmpty();
            assertThat(VersionsAngabe.parseOptional(null)).isEmpty();
        }
    }
}
