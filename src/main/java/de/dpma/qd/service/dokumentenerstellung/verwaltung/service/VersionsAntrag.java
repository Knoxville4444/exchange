package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionArt;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;

/**
 * Antrag auf eine neue Version. {@code inhalt} und {@code schema} sind optional und ersetzen die aus der
 * Basisversion übernommenen Teile.
 */
public record VersionsAntrag(VersionArt art, Integer basisHauptversion, VersionsNummer basisVersion, String kommentar,
                             HochgeladeneDatei inhalt, HochgeladeneDatei schema) {
}
