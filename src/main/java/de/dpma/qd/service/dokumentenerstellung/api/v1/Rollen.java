package de.dpma.qd.service.dokumentenerstellung.api.v1;

/**
 * Rollen des Dienstes. Sie werden in der Konfiguration unter {@code permissions} auf Gruppen bzw. Rollen aus dem Token gemappt und
 * stehen je Endpunkt zusätzlich als {@code x-rollen} in der OpenAPI.
 */
public final class Rollen {

    /** Erzeugung, Konvertierung, Merkblatt, Versionsauflösung. */
    public static final String FORMULAR_NUTZER = "formular_nutzer";

    /** Verwaltung von Kategorien, Vorlagen, Versionen und Veröffentlichungen inkl. Vorschau. */
    public static final String FORMULAR_VERWALTUNG = "formular_verwaltung";

    private Rollen() {
    }
}
