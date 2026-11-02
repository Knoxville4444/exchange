package de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Uhr für Tests, die sich vorstellen lässt („Silvester 0:00“). */
public final class VerstellbareUhr extends Clock {

    private Instant jetzt;

    public VerstellbareUhr(Instant start) {
        this.jetzt = start;
    }

    public void vor(Duration dauer) {
        jetzt = jetzt.plus(dauer);
    }

    public void stelle(Instant zeitpunkt) {
        jetzt = zeitpunkt;
    }

    @Override
    public Instant instant() {
        return jetzt;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
