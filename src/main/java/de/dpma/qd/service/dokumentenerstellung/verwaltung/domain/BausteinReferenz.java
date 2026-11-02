package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import java.util.Objects;

/**
 * Referenz einer Formularversion auf einen Baustein. Eine Hauptversion als Angabe (z. B. {@code 1})
 * bedeutet „neueste veröffentlichte 1.x zum Stichtag“, eine exakte Angabe (z. B. {@code 1.4}) pinnt.
 */
public record BausteinReferenz(BausteinName baustein, VersionsAngabe angabe) {

    public BausteinReferenz {
        Objects.requireNonNull(baustein, "baustein");
        Objects.requireNonNull(angabe, "angabe");
    }
}
