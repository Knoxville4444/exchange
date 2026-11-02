package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVersionStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;

import java.time.Instant;
import java.util.List;

/** Lesesicht auf eine Version zu einem Stichtag. */
public record VersionsAnsicht(String kategorie, String vorlagenName, VorlagenTyp typ, Version version, Instant stichtag,
                              EffektiverVersionStatus effektiverStatus, List<AufgeloesteReferenz> bausteine) {
}
