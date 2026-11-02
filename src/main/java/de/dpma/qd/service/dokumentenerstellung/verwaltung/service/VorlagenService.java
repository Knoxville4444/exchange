package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Zeitraum;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.KategorieRepository;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenFilter;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Anwendungsfälle auf Ebene der Vorlage: anlegen, suchen, Stammdaten, Zeitraum, Verwendungsnachweis. */
@Service
@Transactional
public class VorlagenService {

    private final VorlagenRepository vorlagen;
    private final KategorieRepository kategorien;
    private final VorlagenZugriff zugriff;

    public VorlagenService(VorlagenRepository vorlagen, KategorieRepository kategorien, VorlagenZugriff zugriff) {
        this.vorlagen = vorlagen;
        this.kategorien = kategorien;
        this.zugriff = zugriff;
    }

    @Transactional(readOnly = true)
    public List<VorlagenAnsicht> suche(VorlagenFilter filter, Instant stichtag) {
        Instant t = zugriff.stichtagOderJetzt(stichtag);
        return vorlagen.suche(filter).stream()
                .sorted(Comparator.comparing(Vorlage::vollerName))
                .map(v -> VorlagenAnsicht.von(v, t))
                .toList();
    }

    @Transactional(readOnly = true)
    public VorlagenAnsicht lies(String kategorie, String name, Instant stichtag) {
        return VorlagenAnsicht.von(zugriff.lade(kategorie, name), zugriff.stichtagOderJetzt(stichtag));
    }

    /** Legt eine Vorlage mit Version 1.0 (DRAFT) an. */
    public VorlagenAnsicht legeAn(String kategorie, VorlagenAntrag antrag) {
        kategorien.finde(kategorie).orElseThrow(() -> new NichtGefundenException("KATEGORIE_NICHT_GEFUNDEN",
                "Kategorie " + kategorie + " nicht gefunden"));
        if (vorlagen.finde(kategorie, antrag.name()).isPresent()) {
            throw new KonfliktException("VORLAGE_EXISTIERT", "Vorlage " + kategorie + "/" + antrag.name() + " existiert bereits");
        }
        if (antrag.typ() == null || antrag.inhalt() == null) {
            throw new UngueltigeAnfrageException("PFLICHTFELD_FEHLT", "typ und inhalt sind Pflicht");
        }
        VorlagenTyp typ = antrag.typ();
        InhaltRef schema = null;
        if (typ.istFormular()) {
            if (antrag.schema() == null) {
                throw new UngueltigeAnfrageException("SCHEMA_FEHLT", "Formulare benötigen ein XML-Schema (XSD)");
            }
            schema = zugriff.speichereSchema(antrag.schema());
        } else if (antrag.schema() != null) {
            throw new UngueltigeAnfrageException("SCHEMA_NICHT_ZULAESSIG", typ + " hat kein Schema");
        }
        VorlagenZugriff.VersionsInhalt inhalt = zugriff.speichereVersionsinhalt(typ, antrag.inhalt(), List.of());
        Vorlage vorlage = Vorlage.neu(kategorie, antrag.name(), typ, antrag.bezeichnung(), antrag.beschreibung(),
                antrag.tags(), antrag.kommentar(), inhalt.inhalt(), schema, inhalt.bausteine(), zugriff.neuesAudit());
        return VorlagenAnsicht.von(zugriff.speichere(vorlage), zugriff.jetzt());
    }

    public VorlagenAnsicht aendere(String kategorie, String name, String bezeichnung, String beschreibung, Set<String> tags) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        vorlage.aendereStammdaten(bezeichnung, beschreibung, tags, zugriff.geaendert(vorlage.audit()));
        return VorlagenAnsicht.von(zugriff.speichere(vorlage), zugriff.jetzt());
    }

    public void loesche(String kategorie, String name) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        if (!vorlage.istLoeschbar()) {
            throw new KonfliktException("VORLAGE_NICHT_LOESCHBAR",
                    "Vorlage hat freigegebene Versionen; stattdessen über den Zeitraum zurückziehen");
        }
        if (vorlage.typ() == VorlagenTyp.BAUSTEIN && !zugriff.referenzierendeFormulare(vorlage.alsBausteinName()).isEmpty()) {
            throw new KonfliktException("BAUSTEIN_WIRD_VERWENDET", "Baustein wird noch von Formularen referenziert");
        }
        vorlagen.loesche(vorlage);
    }

    /** Setzt den Zeitraum der gesamten Vorlage ({@code null} entfernt ihn). */
    public VorlagenAnsicht setzeZeitraum(String kategorie, String name, Zeitraum zeitraum) {
        Vorlage vorlage = zugriff.ladeZumAendern(kategorie, name);
        vorlage.setzeVorlagenZeitraum(zeitraum, zugriff.jetzt(), zugriff.geaendert(vorlage.audit()));
        zugriff.pruefeFormular(vorlage);
        zugriff.pruefeBausteinAenderung(vorlage);
        return VorlagenAnsicht.von(zugriff.speichere(vorlage), zugriff.jetzt());
    }

    /** Inhalt der Version, die nach den Regeln der Versionsauflösung zum Stichtag verwendet würde. */
    @Transactional(readOnly = true)
    public InhaltDaten aufgeloesterInhalt(String kategorie, String name, VersionsAngabe angabe, Instant stichtag) {
        Vorlage vorlage = zugriff.lade(kategorie, name);
        Version version = vorlage.loese(angabe, zugriff.stichtagOderJetzt(stichtag))
                .orElseThrow(() -> new NichtGefundenException("KEINE_VERSION",
                        "Keine passende veröffentlichte Version von " + vorlage.vollerName()));
        return new InhaltDaten(version.inhalt(), version.nummer(), dateiname(vorlage, version),
                zugriff.ladeInhalt(version.inhalt()));
    }

    /** Verwendungsnachweis eines Bausteins. */
    @Transactional(readOnly = true)
    public List<Verwendung> verwendungen(String kategorie, String name, Instant stichtag, boolean nurNutzbar) {
        Vorlage baustein = zugriff.lade(kategorie, name);
        zugriff.pruefeTyp(baustein, VorlagenTyp.BAUSTEIN);
        BausteinName bausteinName = baustein.alsBausteinName();
        Instant t = zugriff.stichtagOderJetzt(stichtag);
        return zugriff.referenzierendeFormulare(bausteinName).stream()
                .flatMap(formular -> formular.versionen().stream()
                        .flatMap(v -> v.referenzAuf(bausteinName).stream()
                                .map(ref -> new Verwendung(formular.vollerName(), v.nummer(), v.effektiverStatus(t),
                                        ref.angabe(), baustein.loese(ref.angabe(), t).map(Version::nummer)))))
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
}
