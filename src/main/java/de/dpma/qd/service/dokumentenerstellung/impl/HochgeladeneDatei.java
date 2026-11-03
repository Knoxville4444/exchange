package de.dpma.qd.service.dokumentenerstellung.impl;

import java.util.Objects;

/** Eine hochgeladene Datei (Inhalt oder Schema). */
public record HochgeladeneDatei(String dateiname, byte[] daten) {

    public HochgeladeneDatei {
        Objects.requireNonNull(daten, "daten");
    }
}
