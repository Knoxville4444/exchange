package de.dpma.qd.service.dokumentenerstellung.impl;

/** Geladener Inhalt einer Version inkl. Metadaten für die Auslieferung. */
public record InhaltDaten(InhaltRef ref, VersionsNummer version, String dateiname, byte[] daten) {
}
