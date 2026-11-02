package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler;

import java.util.Map;

/** Kategorie, Vorlage oder Version existiert nicht bzw. ist zum Stichtag nicht nutzbar (HTTP 404). */
public final class NichtGefundenException extends FachlicherFehler {

    public NichtGefundenException(String errorId, String meldung) {
        this(errorId, meldung, Map.of());
    }

    public NichtGefundenException(String errorId, String meldung, Map<String, Object> detail) {
        super(errorId, meldung, detail);
    }
}
