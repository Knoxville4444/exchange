package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVersionStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVorlagenStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Lesesicht auf eine Vorlage zu einem Stichtag. */
public record VorlagenAnsicht(Vorlage vorlage, Instant stichtag, EffektiverVorlagenStatus effektiverStatus,
                              Optional<VersionsNummer> aktuelleVersion, List<VersionKurz> versionen) {

    /** Kurzinfo einer Version, absteigend sortiert. */
    public record VersionKurz(VersionsNummer version, EffektiverVersionStatus effektiverStatus) {
    }

    public static VorlagenAnsicht von(Vorlage vorlage, Instant stichtag) {
        return new VorlagenAnsicht(vorlage, stichtag, vorlage.effektiverStatus(stichtag),
                vorlage.loese(null, stichtag).map(v -> v.nummer()),
                vorlage.versionen().stream().map(v -> new VersionKurz(v.nummer(), v.effektiverStatus(stichtag))).toList());
    }
}
