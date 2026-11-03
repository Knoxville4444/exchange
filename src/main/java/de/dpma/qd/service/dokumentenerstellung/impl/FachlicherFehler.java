package de.dpma.qd.service.dokumentenerstellung.impl;

import java.util.Map;

/**
 * Fachlicher Fehler der Vorlagenverwaltung. Die {@link Art} bestimmt den HTTP-Status, {@link #errorId()} und
 * {@link #detail()} gehen in die {@code RestErrorInfo}.
 */
public class FachlicherFehler extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Art des Fehlers; die Fehlerbehandlung bildet sie auf den HTTP-Status ab. */
    public enum Art {
        /** 404 */
        NICHT_GEFUNDEN,
        /** 409 */
        KONFLIKT,
        /** 400 */
        UNGUELTIG,
        /** 415 */
        FORMAT_NICHT_UNTERSTUETZT
    }

    private final Art art;
    private final String errorId;
    private final transient Map<String, Object> detail;

    public FachlicherFehler(Art art, String errorId, String meldung, Map<String, Object> detail) {
        super(meldung);
        this.art = art;
        this.errorId = errorId;
        this.detail = detail == null ? Map.of() : Map.copyOf(detail);
    }

    public static FachlicherFehler nichtGefunden(String errorId, String meldung) {
        return new FachlicherFehler(Art.NICHT_GEFUNDEN, errorId, meldung, null);
    }

    public static FachlicherFehler vorlageNichtGefunden(String kategorie, String name) {
        return nichtGefunden("VORLAGE_NICHT_GEFUNDEN", "Vorlage " + kategorie + "/" + name + " nicht gefunden");
    }

    public static FachlicherFehler konflikt(String errorId, String meldung) {
        return new FachlicherFehler(Art.KONFLIKT, errorId, meldung, null);
    }

    public static FachlicherFehler konflikt(String errorId, String meldung, Map<String, Object> detail) {
        return new FachlicherFehler(Art.KONFLIKT, errorId, meldung, detail);
    }

    public static FachlicherFehler ungueltig(String errorId, String meldung) {
        return new FachlicherFehler(Art.UNGUELTIG, errorId, meldung, null);
    }

    public static FachlicherFehler ungueltig(String errorId, String meldung, Map<String, Object> detail) {
        return new FachlicherFehler(Art.UNGUELTIG, errorId, meldung, detail);
    }

    public static FachlicherFehler formatNichtUnterstuetzt(String errorId, String meldung) {
        return new FachlicherFehler(Art.FORMAT_NICHT_UNTERSTUETZT, errorId, meldung, null);
    }

    public Art art() {
        return art;
    }

    public String errorId() {
        return errorId;
    }

    public Map<String, Object> detail() {
        return detail;
    }
}
