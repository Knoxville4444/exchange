package de.dpma.qd.service.dokumentenerstellung.impl;

import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.tage;

import java.time.Instant;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZeitraumTest implements WithAssertions {

    @Nested
    class Enthaelt {

        @Test
        void istHalboffen() {
            Zeitraum z = new Zeitraum(tage(1), tage(2));
            assertThat(z.enthaelt(T0)).isFalse();
            assertThat(z.enthaelt(tage(1))).isTrue();
            assertThat(z.enthaelt(tage(2))).isFalse();
        }

        @Test
        void bisMussNachAbLiegen() {
            assertThatThrownBy(() -> new Zeitraum(tage(2), tage(2))).isInstanceOf(FachlicherFehler.class);
        }
    }

    @Nested
    class GleicheVergangenheit {

        private final Instant jetzt = tage(5);

        @Test
        void aenderungenInDerZukunftSindErlaubt() {
            assertThat(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(1), null), new Zeitraum(tage(1), tage(10)), jetzt)).isTrue();
            assertThat(Zeitraum.gleicheVergangenheit(null, new Zeitraum(null, tage(6)), jetzt)).isTrue();
            assertThat(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(8), null), new Zeitraum(tage(9), null), jetzt)).isTrue();
        }

        @Test
        void aenderungenDerVergangenheitWerdenErkannt() {
            assertThat(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(1), null), new Zeitraum(tage(2), null), jetzt)).isFalse();
            assertThat(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(1), tage(3)), new Zeitraum(tage(1), tage(4)), jetzt)).isFalse();
            assertThat(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(8), null), null, jetzt)).isFalse();
        }
    }
}
