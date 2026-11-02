package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import java.time.Instant;

/** Wer hat wann angelegt bzw. zuletzt geändert. */
public record Audit(Instant erstelltAm, String erstelltVon, Instant geaendertAm, String geaendertVon) {

    public static Audit neu(Instant am, String von) {
        return new Audit(am, von, null, null);
    }

    public Audit geaendert(Instant am, String von) {
        return new Audit(erstelltAm, erstelltVon, am, von);
    }
}
