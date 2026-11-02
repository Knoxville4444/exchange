package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;

/**
 * Ein aufgelöster Baustein.
 *
 * @param bereichsname Name des LibreOffice-Bereichs im Formular-ODT, z. B. {@code baustein:PATGBM/Briefkopf}
 * @param inhalt       ODT des Bausteins, dessen Inhalt in den Bereich eingesetzt wird
 */
public record AufgeloesterBaustein(BausteinName baustein, String bereichsname, VersionsNummer version, byte[] inhalt) {
}
