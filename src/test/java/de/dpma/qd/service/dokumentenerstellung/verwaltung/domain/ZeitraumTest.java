package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.T0;
import static de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Testdaten.tage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeitraumTest {

    @Test
    void istHalboffen() {
        Zeitraum z = new Zeitraum(tage(1), tage(2));
        assertFalse(z.enthaelt(T0));
        assertTrue(z.enthaelt(tage(1)));
        assertFalse(z.enthaelt(tage(2)));
    }

    @Test
    void bisMussNachAbLiegen() {
        assertThrows(UngueltigeAnfrageException.class, () -> new Zeitraum(tage(2), tage(2)));
    }

    @Test
    void aenderungenInDerZukunftLassenDieVergangenheitUnveraendert() {
        Instant jetzt = tage(5);
        assertTrue(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(1), null), new Zeitraum(tage(1), tage(10)), jetzt));
        assertTrue(Zeitraum.gleicheVergangenheit(null, new Zeitraum(null, tage(6)), jetzt));
        assertTrue(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(8), null), new Zeitraum(tage(9), null), jetzt));
    }

    @Test
    void aenderungenDerVergangenheitWerdenErkannt() {
        Instant jetzt = tage(5);
        assertFalse(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(1), null), new Zeitraum(tage(2), null), jetzt));
        assertFalse(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(1), tage(3)), new Zeitraum(tage(1), tage(4)), jetzt));
        assertFalse(Zeitraum.gleicheVergangenheit(new Zeitraum(tage(8), null), null, jetzt));
    }
}
