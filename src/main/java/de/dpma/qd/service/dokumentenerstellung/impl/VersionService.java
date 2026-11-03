package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

/**
 * Versionen anlegen, lesen und löschen sowie Inhalt, Schema und Baustein-Referenzen einer Version pflegen.
 * <p>
 * Regeln:
 * <ul>
 *     <li>Die Versionsnummer vergibt der Service: HAUPT → {@code n.0}, NEBEN → {@code n.m+1} (auch für ältere Hauptversionen).</li>
 *     <li>Eine neue Version ohne Upload ist eine Kopie von Inhalt, Schema und Bausteinen der Basisversion.</li>
 *     <li>Nebenversionen übernehmen das Schema ihrer Hauptversion.</li>
 *     <li>Inhalt, Schema und Bausteine sind nur im Status DRAFT änderbar.</li>
 * </ul>
 */
@Service
@Transactional
public class VersionService {

    private final VorlageRepository vorlagen;
    private final InhaltRepository inhaltRepository;
    private final InhaltService inhalte;
    private final BausteinService bausteine;
    private final AuditErzeuger audit;

    public VersionService(VorlageRepository vorlagen, InhaltRepository inhaltRepository, InhaltService inhalte, BausteinService bausteine,
            AuditErzeuger audit) {
        this.vorlagen = vorlagen;
        this.inhaltRepository = inhaltRepository;
        this.inhalte = inhalte;
        this.bausteine = bausteine;
        this.audit = audit;
    }

    /**
     * Antrag auf eine neue Version. {@code inhalt} und {@code schema} sind optional und ersetzen die aus der Basisversion
     * übernommenen Teile.
     */
    public record Antrag(VersionArt art, Integer basisHauptversion, VersionsNummer basisVersion, String kommentar, HochgeladeneDatei inhalt,
            HochgeladeneDatei schema) {
    }

    @Transactional(readOnly = true)
    public List<VersionsAnsicht> liste(String kategorie, String name, Instant stichtag) {
        Vorlage vorlage = lade(kategorie, name);
        Instant t = audit.stichtagOderJetzt(stichtag);
        return vorlage.versionen().stream().map(v -> bausteine.ansicht(vorlage, v, t)).toList();
    }

    @Transactional(readOnly = true)
    public VersionsAnsicht lies(String kategorie, String name, VersionsNummer nummer, Instant stichtag) {
        Vorlage vorlage = lade(kategorie, name);
        return bausteine.ansicht(vorlage, vorlage.version(nummer), audit.stichtagOderJetzt(stichtag));
    }

    /** Legt eine neue Version im Status DRAFT an. */
    public VersionsAnsicht legeAn(String kategorie, String name, Antrag antrag) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        VersionsNummer nummer = naechsteNummer(vorlage, antrag.art(), antrag.basisHauptversion());
        Optional<Version> basis = basisversion(vorlage, nummer, antrag.basisVersion());

        InhaltService.VersionsInhalt inhalt = antrag.inhalt() != null
                ? inhalte.speichereVersionsinhalt(vorlage.typ(), antrag.inhalt(), basis.map(Version::bausteine).orElse(List.of()))
                : basis.map(b -> new InhaltService.VersionsInhalt(b.inhalt(), b.bausteine()))
                        .orElseThrow(() -> FachlicherFehler.ungueltig("INHALT_FEHLT", "Keine Basisversion vorhanden; inhalt muss mitgeschickt werden"));
        InhaltRef schema = schemaFuerNeueVersion(vorlage, nummer, basis, antrag.schema());

        Audit neu = audit.geaendert(vorlage.audit());
        Version version = Version.entwurf(nummer, antrag.kommentar(), inhalt.inhalt(), schema, inhalt.bausteine(), neu);
        pruefeInhaltPasstZumTyp(vorlage.typ(), version);
        return speichere(entity, vorlage.mitVersion(version).mitZeitraum(vorlage.veroeffentlichung(), neu), nummer);
    }

    /** Löscht eine Version im Status DRAFT. Bei Bausteinen darf danach keine Referenz ins Leere zeigen. */
    public void loesche(String kategorie, String name, VersionsNummer nummer) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        pruefeDraft(vorlage.version(nummer));
        Vorlage ohne = vorlage.ohneVersion(nummer);
        bausteine.pruefeKeineVerwaistenReferenzen(ohne);
        VorlageEntityMapper.uebertrage(ohne, entity, vorlagen, inhaltRepository);
        vorlagen.saveAndFlush(entity);
    }

    // --- Inhalt ---------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InhaltDaten inhalt(String kategorie, String name, VersionsNummer nummer) {
        Vorlage vorlage = lade(kategorie, name);
        Version version = vorlage.version(nummer);
        return new InhaltDaten(version.inhalt(), nummer, VorlagenService.dateiname(vorlage, version), inhalte.lade(version.inhalt()));
    }

    /** Ersetzt den Inhalt eines Entwurfs; bei Formularen werden die Baustein-Referenzen neu abgeglichen. */
    public VersionsAnsicht ersetzeInhalt(String kategorie, String name, VersionsNummer nummer, HochgeladeneDatei datei) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        Version version = vorlage.version(nummer);
        pruefeDraft(version);
        InhaltService.VersionsInhalt inhalt = inhalte.speichereVersionsinhalt(vorlage.typ(), datei, version.bausteine());
        return speichere(entity, vorlage.mitVersion(version.mitInhalt(inhalt.inhalt(), inhalt.bausteine(), audit.geaendert(version.audit()))), nummer);
    }

    // --- Schema ---------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] schema(String kategorie, String name, VersionsNummer nummer) {
        Vorlage vorlage = lade(kategorie, name);
        pruefeFormular(vorlage);
        return inhalte.lade(vorlage.version(nummer).schema());
    }

    /**
     * Ersetzt das Schema einer Hauptversion. Es gilt für alle Versionen dieser Hauptversion und ist daher nur änderbar, solange keine
     * davon freigegeben wurde.
     */
    public VersionsAnsicht ersetzeSchema(String kategorie, String name, VersionsNummer nummer, HochgeladeneDatei datei) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        pruefeFormular(vorlage);
        pruefeDraft(vorlage.version(nummer));
        if (!nummer.istHauptversion()) {
            throw FachlicherFehler.konflikt("SCHEMA_NUR_HAUPTVERSION", "Das Schema wird an der Hauptversion " + nummer.haupt() + ".0 gepflegt");
        }
        List<Version> derHauptversion = vorlage.versionen().stream().filter(v -> v.nummer().haupt() == nummer.haupt()).toList();
        if (derHauptversion.stream().anyMatch(v -> !v.istDraft())) {
            throw FachlicherFehler.konflikt("SCHEMA_IN_VERWENDUNG",
                    "Hauptversion " + nummer.haupt() + " hat bereits freigegebene Versionen; das Schema ist unveränderlich");
        }
        InhaltRef schema = inhalte.speichereSchema(datei);
        Vorlage geaendert = vorlage;
        for (Version v : derHauptversion) {
            geaendert = geaendert.mitVersion(v.mitSchema(schema, audit.geaendert(v.audit())));
        }
        return speichere(entity, geaendert, nummer);
    }

    // --- Baustein-Referenzen --------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AufgeloesteReferenz> bausteine(String kategorie, String name, VersionsNummer nummer, Instant stichtag) {
        Vorlage vorlage = lade(kategorie, name);
        pruefeFormular(vorlage);
        return bausteine.loese(vorlage.version(nummer), audit.stichtagOderJetzt(stichtag));
    }

    /** Setzt die Baustein-Referenzen eines Entwurfs, z. B. Pinning auf eine exakte Version. */
    public VersionsAnsicht setzeBausteine(String kategorie, String name, VersionsNummer nummer, List<BausteinReferenz> referenzen) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        pruefeFormular(vorlage);
        Version version = vorlage.version(nummer);
        pruefeDraft(version);
        Set<BausteinName> bereiche = InhaltPruefung.bausteinBereiche(inhalte.lade(version.inhalt()));
        List<BausteinReferenz> geprueft = bausteine.pruefeReferenzen(bereiche, referenzen);
        return speichere(entity, vorlage.mitVersion(version.mitBausteinen(geprueft, audit.geaendert(version.audit()))), nummer);
    }

    // --- Regeln ---------------------------------------------------------------------------------------------------

    private static VersionsNummer naechsteNummer(Vorlage vorlage, VersionArt art, Integer basisHauptversion) {
        if (art == null) {
            throw FachlicherFehler.ungueltig("PFLICHTFELD_FEHLT", "art ist Pflicht");
        }
        if (art == VersionArt.HAUPT) {
            if (basisHauptversion != null) {
                throw FachlicherFehler.ungueltig("BASIS_HAUPTVERSION_NICHT_ZULAESSIG", "basisHauptversion ist nur bei art=NEBEN zulässig");
            }
            return new VersionsNummer(vorlage.hoechsteHauptversion().orElse(0) + 1, 0);
        }
        int haupt = basisHauptversion != null ? basisHauptversion
                : vorlage.hoechsteHauptversion().orElseThrow(() -> FachlicherFehler.ungueltig("KEINE_HAUPTVERSION",
                        "Für " + vorlage.vollerName() + " existiert keine Hauptversion; zuerst art=HAUPT anlegen"));
        return vorlage.versionen().stream().map(Version::nummer).filter(n -> n.haupt() == haupt).max(Comparator.naturalOrder())
                .map(VersionsNummer::naechsteNebenversion)
                .orElseThrow(() -> FachlicherFehler.nichtGefunden("HAUPTVERSION_NICHT_GEFUNDEN",
                        "Hauptversion " + haupt + " von " + vorlage.vollerName() + " existiert nicht"));
    }

    /** Version, aus der kopiert wird: die angegebene, sonst die höchste der Hauptversion (NEBEN) bzw. die höchste überhaupt (HAUPT). */
    private static Optional<Version> basisversion(Vorlage vorlage, VersionsNummer neu, VersionsNummer angegeben) {
        if (angegeben != null) {
            Version basis = vorlage.version(angegeben);
            if (!neu.istHauptversion() && basis.nummer().haupt() != neu.haupt()) {
                throw FachlicherFehler.ungueltig("BASIS_VERSION_PASST_NICHT", "basisVersion " + angegeben + " gehört nicht zur Hauptversion " + neu.haupt());
            }
            return Optional.of(basis);
        }
        if (neu.istHauptversion()) {
            return vorlage.hoechsteVersion();
        }
        return vorlage.versionen().stream().filter(v -> v.nummer().haupt() == neu.haupt()).findFirst();
    }

    private InhaltRef schemaFuerNeueVersion(Vorlage vorlage, VersionsNummer nummer, Optional<Version> basis, HochgeladeneDatei schema) {
        if (!vorlage.typ().istFormular()) {
            if (schema != null) {
                throw FachlicherFehler.ungueltig("SCHEMA_NICHT_ZULAESSIG", vorlage.typ() + " hat kein Schema");
            }
            return null;
        }
        if (!nummer.istHauptversion()) {
            if (schema != null) {
                throw FachlicherFehler.ungueltig("SCHEMA_NICHT_ZULAESSIG", "Nebenversionen übernehmen das Schema ihrer Hauptversion");
            }
            return vorlage.schemaDerHauptversion(nummer.haupt()).orElse(null);
        }
        if (schema != null) {
            return inhalte.speichereSchema(schema);
        }
        return basis.map(Version::schema)
                .orElseThrow(() -> FachlicherFehler.ungueltig("SCHEMA_FEHLT", "Keine Basisversion vorhanden; schema muss mitgeschickt werden"));
    }

    static void pruefeInhaltPasstZumTyp(VorlagenTyp typ, Version version) {
        if (version.inhalt().format() != typ.inhaltsformat()) {
            throw FachlicherFehler.ungueltig("INHALT_PASST_NICHT_ZUM_TYP", typ + " erwartet " + typ.inhaltsformat());
        }
        if (typ.istFormular() && version.schema() == null) {
            throw FachlicherFehler.ungueltig("SCHEMA_FEHLT", "Formulare benötigen ein XML-Schema (XSD)");
        }
    }

    static void pruefeDraft(Version version) {
        if (!version.istDraft()) {
            throw FachlicherFehler.konflikt("VERSION_NICHT_DRAFT", "Version " + version.nummer() + " ist nicht im Status DRAFT und damit unveränderlich");
        }
    }

    private static void pruefeFormular(Vorlage vorlage) {
        if (!vorlage.typ().istFormular()) {
            throw FachlicherFehler.konflikt("TYP_UNPASSEND", "Nur Formulare haben Schema und Baustein-Referenzen");
        }
    }

    private Vorlage lade(String kategorie, String name) {
        return vorlagen.finde(kategorie, name).map(VorlageEntityMapper::zuFachobjekt)
                .orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
    }

    private VorlageEntity ladeZumAendern(String kategorie, String name) {
        return vorlagen.findeZumAendern(kategorie, name).orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
    }

    private VersionsAnsicht speichere(VorlageEntity entity, Vorlage vorlage, VersionsNummer nummer) {
        VorlageEntityMapper.uebertrage(vorlage, entity, vorlagen, inhaltRepository);
        Vorlage gespeichert = VorlageEntityMapper.zuFachobjekt(vorlagen.saveAndFlush(entity));
        return bausteine.ansicht(gespeichert, gespeichert.version(nummer), audit.jetzt());
    }
}
