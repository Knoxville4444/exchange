package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionsNummerTest {

    @Test
    void vergleichtNumerischStattAlsText() {
        assertTrue(VersionsNummer.parse("1.10").compareTo(VersionsNummer.parse("1.9")) > 0);
        assertTrue(VersionsNummer.parse("10.0").compareTo(VersionsNummer.parse("9.99")) > 0);
        assertEquals(List.of("1.2", "1.10", "2.0"),
                List.of("2.0", "1.10", "1.2").stream().map(VersionsNummer::parse).sorted().map(Object::toString).toList());
    }

    @Test
    void parseLehntUngueltigeFormateAb() {
        for (String ungueltig : List.of("", "1", "0.1", "1.", ".1", "1.2.3", "a.b", "-1.0")) {
            assertThrows(UngueltigeAnfrageException.class, () -> VersionsNummer.parse(ungueltig), ungueltig);
        }
    }

    @Test
    void angabeHauptversionPasstAufAlleNebenversionen() {
        VersionsAngabe zwei = VersionsAngabe.parse("2");
        assertFalse(zwei.istExakt());
        assertTrue(zwei.passtZu(VersionsNummer.parse("2.0")));
        assertTrue(zwei.passtZu(VersionsNummer.parse("2.7")));
        assertFalse(zwei.passtZu(VersionsNummer.parse("3.0")));
    }

    @Test
    void exakteAngabePasstNurAufDieseVersion() {
        VersionsAngabe exakt = VersionsAngabe.parse("2.3");
        assertTrue(exakt.istExakt());
        assertTrue(exakt.passtZu(VersionsNummer.parse("2.3")));
        assertFalse(exakt.passtZu(VersionsNummer.parse("2.4")));
    }

    @Test
    void leereAngabeBedeutetKeineEinschraenkung() {
        assertEquals(Optional.empty(), VersionsAngabe.parseOptional(" "));
        assertEquals(Optional.empty(), VersionsAngabe.parseOptional(null));
    }
}
