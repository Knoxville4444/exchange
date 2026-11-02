package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;

/** Geladener Inhalt einer Version inkl. Metadaten für die Auslieferung. */
public record InhaltDaten(InhaltRef ref, VersionsNummer version, String dateiname, byte[] daten) {
}
