package de.dpma.qd.service.dokumentenerstellung.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Erzeugt minimale ODT-, PDF- und XSD-Dateien für Tests. */
public final class TestDateien {

    private static final String KOPF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
            + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"><office:body><office:text>";
    private static final String FUSS = "</office:text></office:body></office:document-content>";

    private TestDateien() {
    }

    /** ODT mit je einem Bereich pro Name in content.xml, z. B. {@code baustein:PATGBM/Briefkopf}. */
    public static HochgeladeneDatei odt(String text, String... bereiche) {
        return new HochgeladeneDatei("vorlage.odt", odtBytes(text, bereiche, new String[0]));
    }

    /** ODT mit Bereichen in content.xml und styles.xml (Kopf-/Fußzeile). */
    public static byte[] odtBytes(String text, String[] bereicheInhalt, String[] bereicheStyles) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            eintrag(zip, "mimetype", "application/vnd.oasis.opendocument.text");
            eintrag(zip, "content.xml", KOPF + "<text:p>" + text + "</text:p>" + bereiche(bereicheInhalt) + FUSS);
            eintrag(zip, "styles.xml", KOPF.replace("document-content", "document-styles") + bereiche(bereicheStyles)
                    + FUSS.replace("document-content", "document-styles"));
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static HochgeladeneDatei pdf() {
        return new HochgeladeneDatei("merkblatt.pdf", "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII));
    }

    public static HochgeladeneDatei xsd() {
        return xsd("aktenzeichen");
    }

    public static HochgeladeneDatei xsd(String feld) {
        String schema = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"formular\"><xs:complexType><xs:sequence>"
                + "<xs:element name=\"" + feld + "\" type=\"xs:string\"/>"
                + "</xs:sequence></xs:complexType></xs:element></xs:schema>";
        return new HochgeladeneDatei("schema.xsd", schema.getBytes(StandardCharsets.UTF_8));
    }

    private static String bereiche(String[] namen) {
        StringBuilder sb = new StringBuilder();
        for (String name : namen) {
            sb.append("<text:section text:name=\"").append(name).append("\"><text:p>Platzhalter</text:p></text:section>");
        }
        return sb.toString();
    }

    private static void eintrag(ZipOutputStream zip, String name, String inhalt) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(inhalt.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
