package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler;

import java.util.Map;

/** Anfrage syntaktisch oder fachlich fehlerhaft (HTTP 400). */
public final class UngueltigeAnfrageException extends FachlicherFehler {

    public UngueltigeAnfrageException(String errorId, String meldung) {
        this(errorId, meldung, Map.of());
    }

    public UngueltigeAnfrageException(String errorId, String meldung, Map<String, Object> detail) {
        super(errorId, meldung, detail);
    }
}
