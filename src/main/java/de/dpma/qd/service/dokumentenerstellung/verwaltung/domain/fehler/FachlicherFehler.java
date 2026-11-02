package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler;

import java.util.Map;

/**
 * Basisklasse aller fachlichen Fehler der Vorlagenverwaltung.
 * <p>
 * Die API-Schicht bildet die Unterklassen auf HTTP-Status ab und überträgt {@link #errorId()} und
 * {@link #detail()} in {@code RestErrorInfo}.
 */
public abstract sealed class FachlicherFehler extends RuntimeException
        permits NichtGefundenException, KonfliktException, UngueltigeAnfrageException, NichtUnterstuetzterInhaltException {

    private final String errorId;
    private final transient Map<String, Object> detail;

    protected FachlicherFehler(String errorId, String meldung, Map<String, Object> detail) {
        super(meldung);
        this.errorId = errorId;
        this.detail = detail == null ? Map.of() : Map.copyOf(detail);
    }

    public String errorId() {
        return errorId;
    }

    public Map<String, Object> detail() {
        return detail;
    }
}
