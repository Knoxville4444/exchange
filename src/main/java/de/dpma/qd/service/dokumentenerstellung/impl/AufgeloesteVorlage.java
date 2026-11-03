package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aufgelöste Vorlage für die Erzeugung.
 *
 * @param inhalt    ODT (Formular) bzw. PDF (Merkblatt)
 * @param schema    XSD zur Validierung der Variablen; nur bei Formularen
 * @param bausteine einzusetzende Bausteine, je Bereich im Formular-ODT
 */
public record AufgeloesteVorlage(String kategorie, String name, VorlagenTyp typ, VersionsNummer version, Instant stichtag, byte[] inhalt,
        byte[] schema, List<Baustein> bausteine) {

    /**
     * Ein aufgelöster Baustein.
     *
     * @param bereichsname Name des LibreOffice-Bereichs im Formular-ODT, z. B. {@code baustein:PATGBM/Briefkopf}
     * @param inhalt       ODT des Bausteins, dessen Inhalt in den Bereich eingesetzt wird
     */
    public record Baustein(BausteinName name, String bereichsname, VersionsNummer version, byte[] inhalt) {
    }

    /** Wert für den Antwort-Header {@code X-Formular-Bausteine}, z. B. {@code PATGBM/Briefkopf=1.4}. */
    public String bausteinHeader() {
        return bausteine.stream().map(b -> b.name() + "=" + b.version()).collect(Collectors.joining(", "));
    }
}
