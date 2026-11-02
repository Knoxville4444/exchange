package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Kategorie;

/** Kategorie mit Anzahl ihrer Vorlagen. */
public record KategorieAnsicht(Kategorie kategorie, long anzahlVorlagen) {
}
