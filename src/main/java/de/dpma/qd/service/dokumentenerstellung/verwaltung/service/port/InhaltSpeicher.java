package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Inhaltsformat;

/**
 * Port für die Ablage der Inhalte (ODT, PDF, XSD). Aktuell als BLOB in Oracle; ein Dateisystem- oder
 * S3-Adapter kann ihn später ersetzen, ohne dass sich die Service-Schicht ändert.
 * <p>
 * Inhalte sind unveränderlich und werden über ihre Prüfsumme dedupliziert: Gleicher Inhalt ergibt
 * dieselbe Referenz.
 */
public interface InhaltSpeicher {

    InhaltRef speichere(byte[] daten, Inhaltsformat format);

    byte[] lade(InhaltRef ref);
}
