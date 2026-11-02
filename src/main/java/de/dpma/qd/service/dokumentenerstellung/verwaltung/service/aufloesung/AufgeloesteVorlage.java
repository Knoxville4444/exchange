package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;

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
public record AufgeloesteVorlage(String kategorie, String name, VorlagenTyp typ, VersionsNummer version, Instant stichtag,
                                 byte[] inhalt, byte[] schema, List<AufgeloesterBaustein> bausteine) {

    /** Wert für den Antwort-Header {@code X-Formular-Bausteine}, z. B. {@code PATGBM/Briefkopf=1.4}. */
    public String bausteinHeader() {
        return bausteine.stream().map(b -> b.baustein() + "=" + b.version()).collect(Collectors.joining(", "));
    }
}
