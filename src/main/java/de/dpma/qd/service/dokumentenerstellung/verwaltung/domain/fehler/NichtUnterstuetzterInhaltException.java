package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler;

import java.util.Map;

/** Hochgeladener Inhalt hat nicht das zum Vorlagentyp passende Format (HTTP 415). */
public final class NichtUnterstuetzterInhaltException extends FachlicherFehler {

    public NichtUnterstuetzterInhaltException(String errorId, String meldung) {
        super(errorId, meldung, Map.of());
    }
}
