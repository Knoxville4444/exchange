package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;

import java.util.Set;

/** Antrag auf Anlage einer Vorlage inkl. Inhalt der Version 1.0. */
public record VorlagenAntrag(String name, VorlagenTyp typ, String bezeichnung, String beschreibung, Set<String> tags,
                             String kommentar, HochgeladeneDatei inhalt, HochgeladeneDatei schema) {
}
