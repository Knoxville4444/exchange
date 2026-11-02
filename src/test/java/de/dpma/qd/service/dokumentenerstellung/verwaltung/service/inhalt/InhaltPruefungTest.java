package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.inhalt;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Inhaltsformat;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtUnterstuetzterInhaltException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten.TestDateien;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InhaltPruefungTest {

    @Test
    void findetBausteinBereicheInInhaltUndKopfzeilen() {
        byte[] odt = TestDateien.odtBytes("Text",
                new String[]{"baustein:PATGBM/Rechtsbehelf", "normaler-bereich"},
                new String[]{"baustein:ALLGEMEIN/Briefkopf"});
        assertEquals(Set.of(new BausteinName("PATGBM", "Rechtsbehelf"), new BausteinName("ALLGEMEIN", "Briefkopf")),
                InhaltPruefung.bausteinBereiche(odt));
    }

    @Test
    void odtOhneBereicheLiefertLeereMenge() {
        assertEquals(Set.of(), InhaltPruefung.bausteinBereiche(TestDateien.odt("Text").daten()));
    }

    @Test
    void ungueltigerBausteinnameWirdAbgelehnt() {
        byte[] odt = TestDateien.odtBytes("Text", new String[]{"baustein:kleinbuchstaben/X"}, new String[0]);
        assertThrows(UngueltigeAnfrageException.class, () -> InhaltPruefung.bausteinBereiche(odt));
    }

    @Test
    void pdfIstKeinOdt() {
        assertThrows(NichtUnterstuetzterInhaltException.class,
                () -> InhaltPruefung.pruefe(TestDateien.pdf().daten(), Inhaltsformat.ODT));
    }

    @Test
    void odtIstKeinPdf() {
        assertThrows(NichtUnterstuetzterInhaltException.class,
                () -> InhaltPruefung.pruefe(TestDateien.odt("x").daten(), Inhaltsformat.PDF));
        assertDoesNotThrow(() -> InhaltPruefung.pruefe(TestDateien.pdf().daten(), Inhaltsformat.PDF));
    }

    @Test
    void schemaWirdAufKompilierbarkeitGeprueft() {
        assertDoesNotThrow(() -> InhaltPruefung.pruefe(TestDateien.xsd().daten(), Inhaltsformat.XSD));
        byte[] kaputt = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element/>".getBytes(StandardCharsets.UTF_8);
        assertThrows(UngueltigeAnfrageException.class, () -> InhaltPruefung.pruefe(kaputt, Inhaltsformat.XSD));
    }

    @Test
    void schemaDarfKeineExternenRessourcenNachladen() {
        byte[] extern = ("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:import namespace=\"urn:x\" schemaLocation=\"http://example.org/x.xsd\"/></xs:schema>")
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(UngueltigeAnfrageException.class, () -> InhaltPruefung.pruefe(extern, Inhaltsformat.XSD));
    }

    @Test
    void leereDateiWirdAbgelehnt() {
        assertThrows(UngueltigeAnfrageException.class, () -> InhaltPruefung.pruefe(new byte[0], Inhaltsformat.ODT));
    }
}
