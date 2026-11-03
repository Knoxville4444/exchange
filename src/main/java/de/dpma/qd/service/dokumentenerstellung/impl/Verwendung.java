package de.dpma.qd.service.dokumentenerstellung.impl;

import java.util.Optional;

/** Verwendung eines Bausteins in einer Formularversion (Verwendungsnachweis). */
public record Verwendung(String formular, VersionsNummer version, EffektiverVersionStatus effektiverStatus, VersionsAngabe angabe,
        Optional<VersionsNummer> aufgeloesteVersion) {
}
