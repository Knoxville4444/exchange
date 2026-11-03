package de.dpma.qd.service.dokumentenerstellung.impl;

import java.nio.charset.StandardCharsets;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InhaltPruefungTest implements WithAssertions {

    @Nested
    class BausteinBereiche {

        @Test
        void findetBereicheInInhaltUndKopfzeilen() {
            byte[] odt = TestDateien.odtBytes("Text", new String[] {"baustein:PATGBM/Rechtsbehelf", "normaler-bereich"},
                    new String[] {"baustein:ALLGEMEIN/Briefkopf"});
            assertThat(InhaltPruefung.bausteinBereiche(odt))
                    .containsExactlyInAnyOrder(new BausteinName("PATGBM", "Rechtsbehelf"), new BausteinName("ALLGEMEIN", "Briefkopf"));
        }

        @Test
        void odtOhneBereicheLiefertLeereMenge() {
            assertThat(InhaltPruefung.bausteinBereiche(TestDateien.odt("Text").daten())).isEmpty();
        }

        @Test
        void ungueltigerBausteinnameWirdAbgelehnt() {
            byte[] odt = TestDateien.odtBytes("Text", new String[] {"baustein:kleinbuchstaben/X"}, new String[0]);
            assertThatThrownBy(() -> InhaltPruefung.bausteinBereiche(odt)).isInstanceOf(FachlicherFehler.class);
        }
    }

    @Nested
    class Pruefe {

        @Test
        void pdfIstKeinOdt() {
            assertThatThrownBy(() -> InhaltPruefung.pruefe(TestDateien.pdf().daten(), Inhaltsformat.ODT))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.art()).isEqualTo(FachlicherFehler.Art.FORMAT_NICHT_UNTERSTUETZT));
        }

        @Test
        void odtIstKeinPdf() {
            assertThatThrownBy(() -> InhaltPruefung.pruefe(TestDateien.odt("x").daten(), Inhaltsformat.PDF)).isInstanceOf(FachlicherFehler.class);
        }

        @Test
        void schemaWirdAufKompilierbarkeitGeprueft() {
            InhaltPruefung.pruefe(TestDateien.xsd().daten(), Inhaltsformat.XSD);
            byte[] kaputt = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element/>".getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> InhaltPruefung.pruefe(kaputt, Inhaltsformat.XSD)).isInstanceOf(FachlicherFehler.class);
        }

        @Test
        void schemaDarfKeineExternenRessourcenNachladen() {
            byte[] extern = ("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                    + "<xs:import namespace=\"urn:x\" schemaLocation=\"http://example.org/x.xsd\"/></xs:schema>").getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> InhaltPruefung.pruefe(extern, Inhaltsformat.XSD)).isInstanceOf(FachlicherFehler.class);
        }

        @Test
        void leereDateiWirdAbgelehnt() {
            assertThatThrownBy(() -> InhaltPruefung.pruefe(new byte[0], Inhaltsformat.ODT)).isInstanceOf(FachlicherFehler.class);
        }
    }
}
