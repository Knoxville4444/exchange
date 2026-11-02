package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

/**
 * Verweis auf einen gespeicherten Inhalt (ODT, PDF oder XSD). Der Inhalt selbst liegt im
 * Inhaltsspeicher und wird nur bei Bedarf geladen.
 *
 * @param id      technische ID im Inhaltsspeicher
 * @param sha256  Prüfsumme (hex), dient der Nachvollziehbarkeit und Deduplizierung
 */
public record InhaltRef(long id, String sha256, Inhaltsformat format, long groesse) {
}
