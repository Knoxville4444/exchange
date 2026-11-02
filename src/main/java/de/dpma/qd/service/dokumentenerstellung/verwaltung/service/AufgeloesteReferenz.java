package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;

import java.util.Optional;

/** Baustein-Referenz mit der zu einem Stichtag aufgelösten Bausteinversion (leer = nicht auflösbar). */
public record AufgeloesteReferenz(BausteinName baustein, VersionsAngabe angabe, Optional<VersionsNummer> aufgeloesteVersion) {
}
