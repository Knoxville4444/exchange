package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;

import java.time.Instant;

/**
 * <b>Nahtstelle zur Dokumentenerzeugung.</b> Die Erzeugung (LibreOffice) holt hierüber die zu verwendende
 * Vorlage samt aufgelöster Bausteine und setzt die Bausteine selbst in die gleichnamigen Bereiche ein.
 * <p>
 * Fehler: {@code NichtGefundenException} (Vorlage, Version oder Baustein nicht auflösbar → 404),
 * {@code KonfliktException} mit {@code TYP_UNPASSEND} (z. B. Erzeugung aus einem Baustein → 409).
 */
public interface VorlagenAufloesung {

    /**
     * Löst die Vorlage nach den Regeln der Versionsauflösung auf.
     *
     * @param angabe        {@code null} = höchste veröffentlichte Version
     * @param stichtag      {@code null} = jetzt
     * @param erwarteterTyp z. B. {@code FORMULAR} für die Erzeugung, {@code MERKBLATT} für die Auslieferung
     */
    AufgeloesteVorlage loese(String kategorie, String name, VersionsAngabe angabe, Instant stichtag, VorlagenTyp erwarteterTyp);

    /**
     * Für die Vorschau: exakt diese Formularversion, unabhängig von ihrem Status.
     *
     * @param bausteinEntwuerfe {@code true}: Bausteine auf die höchste passende Version auflösen, auch DRAFT/GEPLANT
     */
    AufgeloesteVorlage loeseFuerVorschau(String kategorie, String name, VersionsNummer version, boolean bausteinEntwuerfe,
                                         Instant stichtag);

    /** Wie {@link #loese}, aber ohne Inhalte – für {@code GET …/aufloesung}. */
    AufloesungsErgebnis ermittle(String kategorie, String name, VersionsAngabe angabe, Instant stichtag);
}
