package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler;

import java.util.Map;

/** Operation im aktuellen Zustand nicht zulässig (HTTP 409). */
public final class KonfliktException extends FachlicherFehler {

    public KonfliktException(String errorId, String meldung) {
        this(errorId, meldung, Map.of());
    }

    public KonfliktException(String errorId, String meldung, Map<String, Object> detail) {
        super(errorId, meldung, detail);
    }
}
