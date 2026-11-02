package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.AufgeloesteReferenz;

import java.time.Instant;
import java.util.List;

/** Ergebnis der Versionsauflösung ohne Inhalte. */
public record AufloesungsErgebnis(Instant stichtag, String kategorie, String name, VorlagenTyp typ, VersionsNummer version,
                                  List<AufgeloesteReferenz> bausteine) {
}
