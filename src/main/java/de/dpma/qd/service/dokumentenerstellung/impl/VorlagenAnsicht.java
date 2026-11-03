package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.Optional;

/** Vorlage mit dem Stichtag, zu dem Status und aktuelle Version berechnet werden. */
public record VorlagenAnsicht(Vorlage vorlage, Instant stichtag) {

    public EffektiverVorlagenStatus effektiverStatus() {
        return vorlage.effektiverStatus(stichtag);
    }

    public Optional<VersionsNummer> aktuelleVersion() {
        return vorlage.loese(null, stichtag).map(Version::nummer);
    }
}
