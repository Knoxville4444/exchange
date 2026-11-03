package de.dpma.qd.service.dokumentenerstellung.impl;

/**
 * Verweis auf einen gespeicherten Inhalt (ODT, PDF oder XSD). Die Daten selbst lädt der {@link InhaltService}
 * nur bei Bedarf.
 *
 * @param id     technische ID des Inhalts
 * @param sha256 Prüfsumme (hex), dient der Nachvollziehbarkeit und Deduplizierung
 */
public record InhaltRef(Long id, String sha256, Inhaltsformat format, long groesse) {
}
