package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.List;

/** Version einer Vorlage mit effektivem Status und aufgelösten Bausteinen zu einem Stichtag. */
public record VersionsAnsicht(Vorlage vorlage, Version version, Instant stichtag, List<AufgeloesteReferenz> bausteine) {

    public EffektiverVersionStatus effektiverStatus() {
        return version.effektiverStatus(stichtag);
    }
}
