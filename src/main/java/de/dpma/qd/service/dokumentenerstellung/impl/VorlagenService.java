package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlagenSuche;

/** Vorlagen suchen, anlegen, Stammdaten ändern, löschen; aufgelöster Inhalt und Verwendungsnachweis. */
@Service
@Transactional
public class VorlagenService {

    private final VorlageRepository vorlagen;
    private final KategorieRepository kategorien;
    private final InhaltRepository inhaltRepository;
    private final InhaltService inhalte;
    private final BausteinService bausteine;
    private final AuditErzeuger audit;

    public VorlagenService(VorlageRepository vorlagen, KategorieRepository kategorien, InhaltRepository inhaltRepository,
            InhaltService inhalte, BausteinService bausteine, AuditErzeuger audit) {
        this.vorlagen = vorlagen;
        this.kategorien = kategorien;
        this.inhaltRepository = inhaltRepository;
        this.inhalte = inhalte;
        this.bausteine = bausteine;
        this.audit = audit;
    }

    /** Antrag auf Anlage einer Vorlage inkl. Inhalt der Version 1.0. */
    public record Antrag(String name, VorlagenTyp typ, String bezeichnung, String beschreibung, Set<String> tags, String kommentar,
            HochgeladeneDatei inhalt, HochgeladeneDatei schema) {
    }

    @Transactional(readOnly = true)
    public List<VorlagenAnsicht> suche(Set<String> kategorie, Set<VorlagenTyp> typ, Set<String> tag, String suchtext, Instant stichtag) {
        Instant t = audit.stichtagOderJetzt(stichtag);
        return vorlagen.findAll(VorlagenSuche.nach(kategorie, typ, tag, suchtext)).stream()
                .map(VorlageEntityMapper::zuFachobjekt)
                .sorted(Comparator.comparing(Vorlage::vollerName))
                .map(v -> new VorlagenAnsicht(v, t))
                .toList();
    }

    @Transactional(readOnly = true)
    public VorlagenAnsicht lies(String kategorie, String name, Instant stichtag) {
        return new VorlagenAnsicht(lade(kategorie, name), audit.stichtagOderJetzt(stichtag));
    }

    /** Legt eine Vorlage mit Version 1.0 (DRAFT) an. Bei Formularen werden die Baustein-Bereiche im ODT erkannt. */
    public VorlagenAnsicht legeAn(String kategorie, Antrag antrag) {
        KategorieEntity kategorieEntity = kategorien.findByName(kategorie)
                .orElseThrow(() -> FachlicherFehler.nichtGefunden("KATEGORIE_NICHT_GEFUNDEN", "Kategorie " + kategorie + " nicht gefunden"));
        if (vorlagen.finde(kategorie, antrag.name()).isPresent()) {
            throw FachlicherFehler.konflikt("VORLAGE_EXISTIERT", "Vorlage " + kategorie + "/" + antrag.name() + " existiert bereits");
        }
        VorlagenTyp typ = antrag.typ();
        InhaltRef schema = speichereSchemaFuerNeueVorlage(typ, antrag.schema());
        InhaltService.VersionsInhalt inhalt = inhalte.speichereVersionsinhalt(typ, antrag.inhalt(), List.of());
        Audit neu = audit.neu();
        Version ersteVersion = Version.entwurf(VersionsNummer.erste(), antrag.kommentar(), inhalt.inhalt(), schema, inhalt.bausteine(), neu);
        Vorlage vorlage = new Vorlage(kategorie, antrag.name(), typ, antrag.bezeichnung(), antrag.beschreibung(), antrag.tags(), null, neu,
                List.of(ersteVersion));

        VorlageEntity entity = new VorlageEntity();
        entity.setKategorie(kategorieEntity);
        entity.setName(vorlage.name());
        entity.setTyp(typ);
        VorlageEntityMapper.uebertrage(vorlage, entity, vorlagen, inhaltRepository);
        return new VorlagenAnsicht(VorlageEntityMapper.zuFachobjekt(vorlagen.saveAndFlush(entity)), audit.jetzt());
    }

    public VorlagenAnsicht aendere(String kategorie, String name, String bezeichnung, String beschreibung, Set<String> tags) {
        VorlageEntity entity = vorlagen.findeZumAendern(kategorie, name).orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        Vorlage geaendert = vorlage.mitStammdaten(bezeichnung, beschreibung, tags, audit.geaendert(vorlage.audit()));
        VorlageEntityMapper.uebertrage(geaendert, entity, vorlagen, inhaltRepository);
        return new VorlagenAnsicht(VorlageEntityMapper.zuFachobjekt(vorlagen.saveAndFlush(entity)), audit.jetzt());
    }

    /** Löscht eine Vorlage, solange keine Version je freigegeben wurde und (bei Bausteinen) keine Referenz besteht. */
    public void loesche(String kategorie, String name) {
        VorlageEntity entity = vorlagen.findeZumAendern(kategorie, name).orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        if (!vorlage.istLoeschbar()) {
            throw FachlicherFehler.konflikt("VORLAGE_NICHT_LOESCHBAR", "Vorlage hat freigegebene Versionen; stattdessen über den Zeitraum zurückziehen");
        }
        if (vorlage.typ() == VorlagenTyp.BAUSTEIN && !bausteine.referenzierendeFormulare(vorlage.alsBausteinName()).isEmpty()) {
            throw FachlicherFehler.konflikt("BAUSTEIN_WIRD_VERWENDET", "Baustein wird noch von Formularen referenziert");
        }
        vorlagen.delete(entity);
    }

    /** Inhalt der Version, die nach den Regeln der Versionsauflösung zum Stichtag verwendet würde. */
    @Transactional(readOnly = true)
    public InhaltDaten aufgeloesterInhalt(String kategorie, String name, VersionsAngabe angabe, Instant stichtag) {
        Vorlage vorlage = lade(kategorie, name);
        Version version = vorlage.loese(angabe, audit.stichtagOderJetzt(stichtag))
                .orElseThrow(() -> FachlicherFehler.nichtGefunden("KEINE_VERSION", "Keine passende veröffentlichte Version von " + vorlage.vollerName()));
        return new InhaltDaten(version.inhalt(), version.nummer(), dateiname(vorlage, version), inhalte.lade(version.inhalt()));
    }

    /** Verwendungsnachweis eines Bausteins. */
    @Transactional(readOnly = true)
    public List<Verwendung> verwendungen(String kategorie, String name, Instant stichtag, boolean nurNutzbar) {
        Vorlage baustein = lade(kategorie, name);
        if (baustein.typ() != VorlagenTyp.BAUSTEIN) {
            throw FachlicherFehler.konflikt("TYP_UNPASSEND", baustein.vollerName() + " ist kein Baustein");
        }
        BausteinName bausteinName = baustein.alsBausteinName();
        Instant t = audit.stichtagOderJetzt(stichtag);
        return bausteine.referenzierendeFormulare(bausteinName).stream()
                .flatMap(formular -> formular.versionen().stream()
                        .flatMap(v -> v.referenzAuf(bausteinName).stream()
                                .map(ref -> new Verwendung(formular.vollerName(), v.nummer(), v.effektiverStatus(t), ref.angabe(),
                                        baustein.loese(ref.angabe(), t).map(Version::nummer)))))
                .filter(v -> !nurNutzbar || v.effektiverStatus().istAktivOderGeplant())
                .sorted(Comparator.comparing(Verwendung::formular).thenComparing(Verwendung::version, Comparator.reverseOrder()))
                .toList();
    }

    static String dateiname(Vorlage vorlage, Version version) {
        String endung = switch (vorlage.typ().inhaltsformat()) {
            case ODT -> "odt";
            case PDF -> "pdf";
            case XSD -> "xsd";
        };
        return vorlage.name() + "_" + version.nummer() + "." + endung;
    }

    private Vorlage lade(String kategorie, String name) {
        return vorlagen.finde(kategorie, name).map(VorlageEntityMapper::zuFachobjekt)
                .orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
    }

    private InhaltRef speichereSchemaFuerNeueVorlage(VorlagenTyp typ, HochgeladeneDatei schema) {
        if (!typ.istFormular()) {
            if (schema != null) {
                throw FachlicherFehler.ungueltig("SCHEMA_NICHT_ZULAESSIG", typ + " hat kein Schema");
            }
            return null;
        }
        if (schema == null) {
            throw FachlicherFehler.ungueltig("SCHEMA_FEHLT", "Formulare benötigen ein XML-Schema (XSD)");
        }
        return inhalte.speichereSchema(schema);
    }
}
