package de.dpma.qd.service.dokumentenerstellung.impl;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

/**
 * Prüft hochgeladene Inhalte und liest die Baustein-Bereiche aus ODT-Dateien – ausschließlich mit
 * JDK-Mitteln, ohne LibreOffice.
 * <p>
 * Baustein-Bereiche sind LibreOffice-Bereiche ({@code <text:section text:name="baustein:KAT/Name">}).
 * Gesucht wird in {@code content.xml} und {@code styles.xml} (Kopf- und Fußzeilen, z. B. Briefkopf).
 */
public final class InhaltPruefung {

    static final String ODT_MIMETYPE = Inhaltsformat.ODT.mimeType();
    private static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static final Set<String> XML_MIT_BEREICHEN = Set.of("content.xml", "styles.xml");
    /** Schutz vor ZIP-Bomben: maximale entpackte Größe einer einzelnen XML-Datei. */
    private static final long MAX_XML_GROESSE = 50L * 1024 * 1024;

    private InhaltPruefung() {
    }

    /** Prüft, ob die Daten zum erwarteten Format passen. */
    public static void pruefe(byte[] daten, Inhaltsformat format) {
        if (daten == null || daten.length == 0) {
            throw FachlicherFehler.ungueltig("INHALT_LEER", "Die hochgeladene Datei ist leer");
        }
        switch (format) {
            case ODT -> liesOdt(daten);
            case PDF -> pruefePdf(daten);
            case XSD -> pruefeXsd(daten);
        }
    }

    /** Liefert alle Bausteine, für die das ODT einen Bereich {@code baustein:…} enthält. */
    public static Set<BausteinName> bausteinBereiche(byte[] odt) {
        return liesOdt(odt).bausteine();
    }

    // -------------------------------------------------------------------------------------

    private record OdtInfo(Set<BausteinName> bausteine) {
    }

    private static OdtInfo liesOdt(byte[] daten) {
        boolean mimetypeOk = false;
        Set<BausteinName> bausteine = new TreeSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(daten))) {
            ZipEntry eintrag;
            while ((eintrag = zip.getNextEntry()) != null) {
                if ("mimetype".equals(eintrag.getName())) {
                    mimetypeOk = ODT_MIMETYPE.equals(new String(lies(zip, 1024), StandardCharsets.US_ASCII).trim());
                } else if (XML_MIT_BEREICHEN.contains(eintrag.getName())) {
                    bausteine.addAll(bereicheAus(lies(zip, MAX_XML_GROESSE)));
                }
            }
        } catch (IOException e) {
            throw keinOdt();
        }
        if (!mimetypeOk) {
            throw keinOdt();
        }
        return new OdtInfo(bausteine);
    }

    private static Set<BausteinName> bereicheAus(byte[] xml) {
        Set<BausteinName> ergebnis = new TreeSet<>();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT
                            && TEXT_NS.equals(reader.getNamespaceURI()) && "section".equals(reader.getLocalName())) {
                        String bereich = reader.getAttributeValue(TEXT_NS, "name");
                        if (BausteinName.istBausteinBereich(bereich)) {
                            ergebnis.add(BausteinName.ausBereich(bereich));
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw FachlicherFehler.ungueltig("ODT_DEFEKT", "Das ODT enthält kein lesbares XML: " + e.getMessage());
        }
        return ergebnis;
    }

    private static void pruefePdf(byte[] daten) {
        String kopf = new String(daten, 0, Math.min(daten.length, 5), StandardCharsets.US_ASCII);
        if (!"%PDF-".equals(kopf)) {
            throw FachlicherFehler.formatNichtUnterstuetzt("KEIN_PDF", "Merkblätter müssen als PDF hochgeladen werden");
        }
    }

    private static void pruefeXsd(byte[] daten) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.newSchema(new StreamSource(new ByteArrayInputStream(daten)));
        } catch (org.xml.sax.SAXException e) {
            throw FachlicherFehler.ungueltig("SCHEMA_UNGUELTIG", "Das XML-Schema ist ungültig: " + e.getMessage(),
                    Map.of("fehler", String.valueOf(e.getMessage())));
        }
    }

    private static byte[] lies(InputStream in, long maximum) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] puffer = new byte[8192];
        long gesamt = 0;
        int n;
        while ((n = in.read(puffer)) > 0) {
            gesamt += n;
            if (gesamt > maximum) {
                throw FachlicherFehler.ungueltig("ODT_ZU_GROSS", "Eine Datei im ODT überschreitet die zulässige Größe");
            }
            out.write(puffer, 0, n);
        }
        return out.toByteArray();
    }

    private static FachlicherFehler keinOdt() {
        return FachlicherFehler.formatNichtUnterstuetzt("KEIN_ODT", "Formulare und Bausteine müssen als ODT hochgeladen werden");
    }
}
