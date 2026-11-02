package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVersionStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;

import java.util.Optional;

/** Verwendung eines Bausteins in einer Formularversion (Verwendungsnachweis). */
public record Verwendung(String formular, VersionsNummer version, EffektiverVersionStatus effektiverStatus,
                         VersionsAngabe angabe, Optional<VersionsNummer> aufgeloesteVersion) {
}
