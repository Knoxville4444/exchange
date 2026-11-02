package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinPruefung;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.inhalt.InhaltPruefung;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Anwendungsfälle auf Ebene der Version: anlegen, Inhalt, Schema, Bausteine, Veröffentlichung. */
@Service
@Transactional
public class VersionService {

    private final VorlagenZugriff zugriff;

    public VersionService(VorlagenZugriff zugriff) {
        this.zugriff = zugriff;
    }

    @Transactional(readOnly = true)
    public List<VersionsAnsicht> liste(String kategorie, String name, Instant stichtag) {
        Vorlage vorlage = zugriff.lade(kategorie, name);
        Instant t = zugriff.stichtagOderJetzt(stichtag);
        return vorlage.versionen().stream().map(v -> zugriff.ansicht(vorlage, v, t)).toList();
    }

    @Transactional(readOnly = true)
    public VersionsAnsicht lies(String kategorie, String name, VersionsNummer nummer, Instant stichtag) {
        Vorlage vorlage = zugriff.lade(kategorie, name);
        return zugriff.ansicht(vorlage, vorlage.version(nummer), zugriff.stichtagOderJetzt(stichtag));
    }

    /**
     * Legt eine neue Version an. Inhalt, Schema und Baustein-Referenzen werden aus der Basisversion
     * übernommen; mitgeschickte Teile ersetzen sie.
     */
    public VersionsAnsicht legeAn(String kategorie, String name, VersionsAntrag antrag) {
        if (antrag.art() == null) {
            throw new UngueltigeAnfrageException("PFLICHTFELD_FEHLT", "art ist Pflicht");
        }
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        Vorlage.VersionsPlan plan = vorlage.planeNeueVersion(antrag.art(), antrag.basisHauptversion(), antrag.basisVersion());
        Optional<Version> basis = plan.basis();
        VersionsNummer nummer = plan.nummer();

        VorlagenZugriff.VersionsInhalt inhalt;
        if (antrag.inhalt() != null) {
            inhalt = zugriff.speichereVersionsinhalt(vorlage.typ(), antrag.inhalt(),
                    basis.map(Version::bausteine).orElse(List.of()));
        } else {
            Version b = basis.orElseThrow(() -> new UngueltigeAnfrageException("INHALT_FEHLT",
                    "Keine Basisversion vorhanden; inhalt muss mitgeschickt werden"));
            inhalt = new VorlagenZugriff.VersionsInhalt(b.inhalt(), b.bausteine());
        }

        InhaltRef schema = null;
        if (vorlage.typ().istFormular()) {
            if (!nummer.istHauptversion()) {
                if (antrag.schema() != null) {
                    throw new UngueltigeAnfrageException("SCHEMA_NICHT_ZULAESSIG",
                            "Nebenversionen übernehmen das Schema ihrer Hauptversion");
                }
                schema = vorlage.schemaDerHauptversion(nummer.haupt()).orElse(null);
            } else if (antrag.schema() != null) {
                schema = zugriff.speichereSchema(antrag.schema());
            } else {
                schema = basis.map(Version::schema).orElseThrow(() -> new UngueltigeAnfrageException("SCHEMA_FEHLT",
                        "Keine Basisversion vorhanden; schema muss mitgeschickt werden"));
            }
        } else if (antrag.schema() != null) {
            throw new UngueltigeAnfrageException("SCHEMA_NICHT_ZULAESSIG", vorlage.typ() + " hat kein Schema");
        }

        vorlage.fuegeVersionHinzu(nummer, antrag.kommentar(), inhalt.inhalt(), schema, inhalt.bausteine(),
                zugriff.geaendert(vorlage.audit()));
        Vorlage gespeichert = zugriff.speichere(vorlage);
        return zugriff.ansicht(gespeichert, gespeichert.version(nummer), zugriff.jetzt());
    }

    public void loesche(String kategorie, String name, VersionsNummer nummer) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        vorlage.loescheVersion(nummer);
        zugriff.pruefeKeineVerwaistenReferenzen(vorlage);
        zugriff.speichere(vorlage);
    }

    // --- Inhalt ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InhaltDaten inhalt(String kategorie, String name, VersionsNummer nummer) {
        Vorlage vorlage = zugriff.lade(kategorie, name);
        Version version = vorlage.version(nummer);
        return new InhaltDaten(version.inhalt(), nummer, VorlagenService.dateiname(vorlage, version),
                zugriff.ladeInhalt(version.inhalt()));
    }

    public VersionsAnsicht ersetzeInhalt(String kategorie, String name, VersionsNummer nummer, HochgeladeneDatei datei) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        Version version = vorlage.version(nummer);
        VorlagenZugriff.VersionsInhalt inhalt = zugriff.speichereVersionsinhalt(vorlage.typ(), datei, version.bausteine());
        vorlage.ersetzeInhalt(nummer, inhalt.inhalt(), inhalt.bausteine(), zugriff.geaendert(version.audit()));
        Vorlage gespeichert = zugriff.speichere(vorlage);
        return zugriff.ansicht(gespeichert, gespeichert.version(nummer), zugriff.jetzt());
    }

    // --- Schema ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] schema(String kategorie, String name, VersionsNummer nummer) {
        Vorlage vorlage = zugriff.lade(kategorie, name);
        zugriff.pruefeTyp(vorlage, VorlagenTyp.FORMULAR);
        return zugriff.ladeInhalt(vorlage.version(nummer).schema());
    }

    public VersionsAnsicht ersetzeSchema(String kategorie, String name, VersionsNummer nummer, HochgeladeneDatei datei) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        zugriff.pruefeTyp(vorlage, VorlagenTyp.FORMULAR);
        Version version = vorlage.version(nummer);
        vorlage.ersetzeSchema(nummer, zugriff.speichereSchema(datei), zugriff.geaendert(version.audit()));
        Vorlage gespeichert = zugriff.speichere(vorlage);
        return zugriff.ansicht(gespeichert, gespeichert.version(nummer), zugriff.jetzt());
    }

    // --- Baustein-Referenzen ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AufgeloesteReferenz> bausteine(String kategorie, String name, VersionsNummer nummer, Instant stichtag) {
        Vorlage vorlage = zugriff.lade(kategorie, name);
        zugriff.pruefeTyp(vorlage, VorlagenTyp.FORMULAR);
        return zugriff.ansicht(vorlage, vorlage.version(nummer), zugriff.stichtagOderJetzt(stichtag)).bausteine();
    }

    /** Setzt die Baustein-Referenzen, z. B. Pinning auf eine exakte Version. Nur im Status DRAFT. */
    public VersionsAnsicht setzeBausteine(String kategorie, String name, VersionsNummer nummer,
                                          List<BausteinReferenz> referenzen) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        zugriff.pruefeTyp(vorlage, VorlagenTyp.FORMULAR);
        Version version = vorlage.version(nummer);
        var bereiche = InhaltPruefung.bausteinBereiche(zugriff.ladeInhalt(version.inhalt()));
        List<BausteinReferenz> geprueft = BausteinPruefung.pruefeReferenzen(bereiche, referenzen, zugriff::baustein);
        vorlage.setzeBausteine(nummer, geprueft, zugriff.geaendert(version.audit()));
        Vorlage gespeichert = zugriff.speichere(vorlage);
        return zugriff.ansicht(gespeichert, gespeichert.version(nummer), zugriff.jetzt());
    }

    // --- Veröffentlichung -------------------------------------------------------------------

    /** Veröffentlicht sofort oder geplant, verschiebt den Termin oder zieht zurück. */
    public VersionsAnsicht setzeVeroeffentlichung(String kategorie, String name, VersionsNummer nummer,
                                                  Instant ab, Instant bis) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        Version version = vorlage.version(nummer);
        vorlage.setzeVeroeffentlichung(nummer, ab, bis, zugriff.jetzt(), zugriff.geaendert(version.audit()));
        zugriff.pruefeFormular(vorlage);
        zugriff.pruefeBausteinAenderung(vorlage);
        Vorlage gespeichert = zugriff.speichere(vorlage);
        return zugriff.ansicht(gespeichert, gespeichert.version(nummer), zugriff.jetzt());
    }

    /** Sagt eine geplante Veröffentlichung ab (Version wird wieder DRAFT). */
    public VersionsAnsicht sageVeroeffentlichungAb(String kategorie, String name, VersionsNummer nummer) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        Version version = vorlage.version(nummer);
        vorlage.sageVeroeffentlichungAb(nummer, zugriff.jetzt(), zugriff.geaendert(version.audit()));
        zugriff.pruefeBausteinAenderung(vorlage);
        Vorlage gespeichert = zugriff.speichere(vorlage);
        return zugriff.ansicht(gespeichert, gespeichert.version(nummer), zugriff.jetzt());
    }
}
