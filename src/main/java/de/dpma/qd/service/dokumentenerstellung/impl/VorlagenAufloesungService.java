package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

/**
 * <b>Nahtstelle zur Dokumentenerzeugung.</b> Die Erzeugung (LibreOffice) holt hierüber die zu verwendende Vorlage samt aufgelöster
 * Bausteine und setzt die Bausteine selbst in die gleichnamigen Bereiche ein, bevor sie die Variablen befüllt.
 * <p>
 * Fehler: {@link FachlicherFehler.Art#NICHT_GEFUNDEN} (Vorlage, Version oder Baustein nicht auflösbar), {@link FachlicherFehler.Art#KONFLIKT}
 * mit {@code TYP_UNPASSEND} (z. B. Erzeugung aus einem Baustein).
 */
@Service
@Transactional(readOnly = true)
public class VorlagenAufloesungService {

    private final VorlageRepository vorlagen;
    private final InhaltService inhalte;
    private final BausteinService bausteine;
    private final AuditErzeuger audit;

    public VorlagenAufloesungService(VorlageRepository vorlagen, InhaltService inhalte, BausteinService bausteine, AuditErzeuger audit) {
        this.vorlagen = vorlagen;
        this.inhalte = inhalte;
        this.bausteine = bausteine;
        this.audit = audit;
    }

    /** Ergebnis der Versionsauflösung ohne Inhalte, für {@code GET …/aufloesung}. */
    public record Ergebnis(Instant stichtag, Vorlage vorlage, Version version, List<AufgeloesteReferenz> bausteine) {
    }

    /**
     * Löst die Vorlage nach den Regeln der Versionsauflösung auf und lädt die Inhalte.
     *
     * @param angabe        {@code null} = höchste veröffentlichte Version
     * @param stichtag      {@code null} = jetzt
     * @param erwarteterTyp z. B. {@code FORMULAR} für die Erzeugung, {@code MERKBLATT} für die Auslieferung
     */
    public AufgeloesteVorlage loese(String kategorie, String name, VersionsAngabe angabe, Instant stichtag, VorlagenTyp erwarteterTyp) {
        Instant t = audit.stichtagOderJetzt(stichtag);
        Vorlage vorlage = lade(kategorie, name);
        if (erwarteterTyp != null && vorlage.typ() != erwarteterTyp) {
            throw FachlicherFehler.konflikt("TYP_UNPASSEND", vorlage.vollerName() + " ist vom Typ " + vorlage.typ() + ", erwartet: " + erwarteterTyp);
        }
        Version version = vorlage.loese(angabe, t).orElseThrow(() -> keineVersion(vorlage, angabe, t));
        List<AufgeloesteVorlage.Baustein> geladen = version.bausteine().stream()
                .map(ref -> ladeBaustein(ref, baustein(ref).loese(ref.angabe(), t), t))
                .toList();
        return ergebnis(vorlage, version, t, geladen);
    }

    /**
     * Für die Vorschau: exakt diese Formularversion, unabhängig von ihrem Status.
     *
     * @param bausteinEntwuerfe {@code true}: Bausteine auf die höchste passende Version auflösen, auch DRAFT/GEPLANT
     */
    public AufgeloesteVorlage loeseFuerVorschau(String kategorie, String name, VersionsNummer nummer, boolean bausteinEntwuerfe, Instant stichtag) {
        Instant t = audit.stichtagOderJetzt(stichtag);
        Vorlage vorlage = lade(kategorie, name);
        if (!vorlage.typ().istFormular()) {
            throw FachlicherFehler.konflikt("TYP_UNPASSEND", "Eine Vorschau gibt es nur für Formulare");
        }
        Version version = vorlage.version(nummer);
        List<AufgeloesteVorlage.Baustein> geladen = version.bausteine().stream()
                .map(ref -> {
                    Vorlage b = baustein(ref);
                    return ladeBaustein(ref, bausteinEntwuerfe ? b.hoechstePassendeVersion(ref.angabe()) : b.loese(ref.angabe(), t), t);
                })
                .toList();
        return ergebnis(vorlage, version, t, geladen);
    }

    /** Wie {@link #loese}, aber ohne Inhalte. */
    public Ergebnis ermittle(String kategorie, String name, VersionsAngabe angabe, Instant stichtag) {
        Instant t = audit.stichtagOderJetzt(stichtag);
        Vorlage vorlage = lade(kategorie, name);
        Version version = vorlage.loese(angabe, t).orElseThrow(() -> keineVersion(vorlage, angabe, t));
        return new Ergebnis(t, vorlage, version, bausteine.loese(version, t));
    }

    private Vorlage lade(String kategorie, String name) {
        return vorlagen.finde(kategorie, name).map(VorlageEntityMapper::zuFachobjekt)
                .orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
    }

    private Vorlage baustein(BausteinReferenz ref) {
        return bausteine.finde(ref.baustein())
                .orElseThrow(() -> FachlicherFehler.nichtGefunden("BAUSTEIN_NICHT_AUFLOESBAR", "Baustein " + ref.baustein() + " existiert nicht"));
    }

    private AufgeloesteVorlage.Baustein ladeBaustein(BausteinReferenz ref, Optional<Version> version, Instant t) {
        Version v = version.orElseThrow(() -> FachlicherFehler.nichtGefunden("BAUSTEIN_NICHT_AUFLOESBAR",
                "Baustein " + ref.baustein() + "@" + ref.angabe() + " ist zum " + t + " nicht auflösbar"));
        return new AufgeloesteVorlage.Baustein(ref.baustein(), ref.baustein().bereichsname(), v.nummer(), inhalte.lade(v.inhalt()));
    }

    private AufgeloesteVorlage ergebnis(Vorlage vorlage, Version version, Instant t, List<AufgeloesteVorlage.Baustein> geladen) {
        byte[] schema = version.schema() == null ? null : inhalte.lade(version.schema());
        return new AufgeloesteVorlage(vorlage.kategorie(), vorlage.name(), vorlage.typ(), version.nummer(), t, inhalte.lade(version.inhalt()), schema,
                geladen);
    }

    private static FachlicherFehler keineVersion(Vorlage vorlage, VersionsAngabe angabe, Instant t) {
        return FachlicherFehler.nichtGefunden("KEINE_VERSION",
                "Keine veröffentlichte Version von " + vorlage.vollerName() + (angabe == null ? "" : " passend zu " + angabe) + " zum " + t);
    }
}
