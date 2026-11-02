package de.dpma.qd.service.dokumentenerstellung.verwaltung.service;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Audit;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinPruefung;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinPruefung.FehlendeAufloesung;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Inhaltsformat;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.inhalt.InhaltPruefung;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.AktuellerBenutzer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.InhaltSpeicher;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Gemeinsame Bausteine der Services: Laden mit Fehlerbehandlung, Audit, Inhalte speichern, Ansichten bilden
 * und die Prüfungen zwischen Formularen und Bausteinen anstoßen.
 * <p>
 * Läuft immer innerhalb der Transaktion des aufrufenden Services.
 */
@Component
public class VorlagenZugriff {

    private final VorlagenRepository vorlagen;
    private final InhaltSpeicher inhalte;
    private final AktuellerBenutzer benutzer;
    private final Clock uhr;

    public VorlagenZugriff(VorlagenRepository vorlagen, InhaltSpeicher inhalte, AktuellerBenutzer benutzer, Clock uhr) {
        this.vorlagen = vorlagen;
        this.inhalte = inhalte;
        this.benutzer = benutzer;
        this.uhr = uhr;
    }

    // --- Zeit und Audit --------------------------------------------------------------------

    public Instant jetzt() {
        return uhr.instant();
    }

    public Instant stichtagOderJetzt(Instant stichtag) {
        return stichtag == null ? jetzt() : stichtag;
    }

    public Audit neuesAudit() {
        return Audit.neu(jetzt(), benutzer.kennung());
    }

    public Audit geaendert(Audit audit) {
        return audit.geaendert(jetzt(), benutzer.kennung());
    }

    // --- Laden -----------------------------------------------------------------------------

    public Vorlage lade(String kategorie, String name) {
        return vorlagen.finde(kategorie, name).orElseThrow(() -> nichtGefunden(kategorie, name));
    }

    public Vorlage ladeZumAendern(String kategorie, String name) {
        return vorlagen.findeZumAendern(kategorie, name).orElseThrow(() -> nichtGefunden(kategorie, name));
    }

    public Optional<Vorlage> baustein(BausteinName name) {
        return vorlagen.finde(name.kategorie(), name.name());
    }

    public Vorlage speichere(Vorlage vorlage) {
        return vorlagen.speichere(vorlage);
    }

    public void pruefeTyp(Vorlage vorlage, VorlagenTyp erwartet) {
        if (vorlage.typ() != erwartet) {
            throw new KonfliktException("TYP_UNPASSEND",
                    vorlage.vollerName() + " ist vom Typ " + vorlage.typ() + ", erwartet: " + erwartet);
        }
    }

    // --- Inhalte ---------------------------------------------------------------------------

    public byte[] ladeInhalt(InhaltRef ref) {
        return inhalte.lade(ref);
    }

    public InhaltRef speichereSchema(HochgeladeneDatei datei) {
        return speichere(datei, Inhaltsformat.XSD);
    }

    /** Inhalt einer Version samt abgeglichener Baustein-Referenzen. */
    public record VersionsInhalt(InhaltRef inhalt, List<BausteinReferenz> bausteine) {
    }

    /**
     * Prüft und speichert den Inhalt einer Version. Bei Formularen werden die Baustein-Bereiche erkannt und
     * mit den bisherigen Referenzen abgeglichen; Bausteine dürfen selbst keine Bausteine enthalten.
     */
    public VersionsInhalt speichereVersionsinhalt(VorlagenTyp typ, HochgeladeneDatei datei, List<BausteinReferenz> bisher) {
        InhaltRef ref = speichere(datei, typ.inhaltsformat());
        if (typ.inhaltsformat() != Inhaltsformat.ODT) {
            return new VersionsInhalt(ref, List.of());
        }
        Set<BausteinName> bereiche = InhaltPruefung.bausteinBereiche(datei.daten());
        if (typ == VorlagenTyp.BAUSTEIN) {
            if (!bereiche.isEmpty()) {
                throw new UngueltigeAnfrageException("VERSCHACHTELTE_BAUSTEINE", "Bausteine dürfen keine Bausteine enthalten");
            }
            return new VersionsInhalt(ref, List.of());
        }
        return new VersionsInhalt(ref, BausteinPruefung.gleicheAb(bereiche, bisher, this::baustein));
    }

    private InhaltRef speichere(HochgeladeneDatei datei, Inhaltsformat format) {
        InhaltPruefung.pruefe(datei.daten(), format);
        return inhalte.speichere(datei.daten(), format);
    }

    // --- Ansichten -------------------------------------------------------------------------

    public VersionsAnsicht ansicht(Vorlage vorlage, Version version, Instant stichtag) {
        List<AufgeloesteReferenz> referenzen = version.bausteine().stream()
                .map(ref -> new AufgeloesteReferenz(ref.baustein(), ref.angabe(),
                        baustein(ref.baustein()).flatMap(b -> b.loese(ref.angabe(), stichtag)).map(Version::nummer)))
                .toList();
        return new VersionsAnsicht(vorlage.kategorie(), vorlage.name(), vorlage.typ(), version, stichtag,
                version.effektiverStatus(stichtag), referenzen);
    }

    // --- Prüfungen zwischen Formularen und Bausteinen ----------------------------------------

    /**
     * Prüft alle freigegebenen Versionen eines Formulars gegen die Bausteine. Die referenzierten Bausteine
     * werden dabei gesperrt, damit nicht parallel ein benötigter Baustein zurückgezogen wird.
     */
    public void pruefeFormular(Vorlage formular) {
        if (!formular.typ().istFormular()) {
            return;
        }
        Map<BausteinName, Optional<Vorlage>> gesperrt = new HashMap<>();
        Function<BausteinName, Optional<Vorlage>> quelle = name -> gesperrt.computeIfAbsent(name,
                n -> vorlagen.findeZumAendern(n.kategorie(), n.name()));
        Instant jetzt = jetzt();
        List<FehlendeAufloesung> fehlend = formular.versionen().stream()
                .flatMap(v -> BausteinPruefung.pruefeFormularVersion(formular, v, quelle, jetzt).stream())
                .toList();
        if (!fehlend.isEmpty()) {
            throw new KonfliktException("BAUSTEIN_NICHT_AUFLOESBAR",
                    "Mindestens ein Baustein ist im Veröffentlichungszeitraum nicht auflösbar",
                    Map.of("fehlend", fehlend.stream().map(FehlendeAufloesung::alsDetail).toList()));
        }
    }

    /** Prüft, ob eine geänderte Bausteinvorlage alle Formulare weiterhin bedienen kann. */
    public void pruefeBausteinAenderung(Vorlage bausteinNeu) {
        if (bausteinNeu.typ() != VorlagenTyp.BAUSTEIN) {
            return;
        }
        List<Vorlage> formulare = vorlagen.findeReferenzierende(bausteinNeu.alsBausteinName());
        List<FehlendeAufloesung> fehlend = BausteinPruefung.pruefeBausteinAenderung(bausteinNeu, formulare, jetzt());
        if (!fehlend.isEmpty()) {
            throw new KonfliktException("BAUSTEIN_WIRD_VERWENDET",
                    "Baustein " + bausteinNeu.vollerName() + " wird von veröffentlichten oder geplanten Formularen benötigt",
                    Map.of("verwendungen", fehlend.stream().map(FehlendeAufloesung::alsDetail).toList()));
        }
    }

    /** Prüft nach dem Löschen einer Bausteinversion, dass keine Referenz ins Leere zeigt. */
    public void pruefeKeineVerwaistenReferenzen(Vorlage bausteinNeu) {
        if (bausteinNeu.typ() != VorlagenTyp.BAUSTEIN) {
            return;
        }
        List<String> verwaist = BausteinPruefung.verwaisteReferenzen(bausteinNeu,
                vorlagen.findeReferenzierende(bausteinNeu.alsBausteinName()));
        if (!verwaist.isEmpty()) {
            throw new KonfliktException("BAUSTEIN_WIRD_VERWENDET",
                    "Die Version wird noch von Formularversionen referenziert", Map.of("verwendungen", verwaist));
        }
    }

    public List<Vorlage> referenzierendeFormulare(BausteinName baustein) {
        return vorlagen.findeReferenzierende(baustein);
    }

    private static NichtGefundenException nichtGefunden(String kategorie, String name) {
        return new NichtGefundenException("VORLAGE_NICHT_GEFUNDEN", "Vorlage " + kategorie + "/" + name + " nicht gefunden");
    }
}
