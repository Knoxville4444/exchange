package de.dpma.qd.service.dokumentenerstellung.impl;

import java.util.Optional;

/** Baustein-Referenz mit der zu einem Stichtag aufgelösten Bausteinversion (leer = nicht auflösbar). */
public record AufgeloesteReferenz(BausteinName baustein, VersionsAngabe angabe, Optional<VersionsNummer> aufgeloesteVersion) {
}
