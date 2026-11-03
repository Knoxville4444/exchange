package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import de.dpma.qd.service.dokumentenerstellung.impl.BausteinPruefung.FehlendeAufloesung;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

/**
 * Lookups und Prüfungen zwischen Formularen und Bausteinen. Die eigentlichen Regeln stehen in {@link BausteinPruefung};
 * hier werden die beteiligten Vorlagen geladen und Fehler gemeldet.
 */
@Service
public class BausteinService {

    private final VorlageRepository vorlagen;
    private final AuditErzeuger audit;

    public BausteinService(VorlageRepository vorlagen, AuditErzeuger audit) {
        this.vorlagen = vorlagen;
        this.audit = audit;
    }

    public Optional<Vorlage> finde(BausteinName name) {
        return vorlagen.finde(name.kategorie(), name.name()).map(VorlageEntityMapper::zuFachobjekt);
    }

    public List<Vorlage> referenzierendeFormulare(BausteinName baustein) {
        return vorlagen.findeReferenzierende(baustein.kategorie(), baustein.name()).stream().map(VorlageEntityMapper::zuFachobjekt).toList();
    }

    /** Referenzen einer Version mit der zum Stichtag aufgelösten Bausteinversion. */
    public List<AufgeloesteReferenz> loese(Version version, Instant stichtag) {
        return version.bausteine().stream()
                .map(ref -> new AufgeloesteReferenz(ref.baustein(), ref.angabe(),
                        finde(ref.baustein()).flatMap(b -> b.loese(ref.angabe(), stichtag)).map(Version::nummer)))
                .toList();
    }

    public VersionsAnsicht ansicht(Vorlage vorlage, Version version, Instant stichtag) {
        return new VersionsAnsicht(vorlage, version, stichtag, loese(version, stichtag));
    }

    /** Explizit gesetzte Referenzen gegen die Bereiche im ODT und die existierenden Bausteine prüfen. */
    public List<BausteinReferenz> pruefeReferenzen(Set<BausteinName> bereiche, List<BausteinReferenz> referenzen) {
        return BausteinPruefung.pruefeReferenzen(bereiche, referenzen, this::finde);
    }

    /**
     * Prüft alle freigegebenen Versionen eines Formulars gegen die Bausteine. Die referenzierten Bausteine werden dabei gesperrt,
     * damit nicht parallel ein benötigter Baustein zurückgezogen wird.
     */
    public void pruefeFormular(Vorlage formular) {
        if (!formular.typ().istFormular()) {
            return;
        }
        Map<BausteinName, Optional<Vorlage>> gesperrt = new HashMap<>();
        Function<BausteinName, Optional<Vorlage>> quelle = name -> gesperrt.computeIfAbsent(name,
                n -> vorlagen.findeZumAendern(n.kategorie(), n.name()).map(VorlageEntityMapper::zuFachobjekt));
        Instant jetzt = audit.jetzt();
        List<FehlendeAufloesung> fehlend = formular.versionen().stream()
                .flatMap(v -> BausteinPruefung.pruefeFormularVersion(formular, v, quelle, jetzt).stream())
                .toList();
        if (!fehlend.isEmpty()) {
            throw FachlicherFehler.konflikt("BAUSTEIN_NICHT_AUFLOESBAR", "Mindestens ein Baustein ist im Veröffentlichungszeitraum nicht auflösbar",
                    Map.of("fehlend", fehlend.stream().map(FehlendeAufloesung::alsDetail).toList()));
        }
    }

    /** Prüft, ob eine geänderte Bausteinvorlage alle Formulare weiterhin bedienen kann. */
    public void pruefeBausteinAenderung(Vorlage bausteinNeu) {
        if (bausteinNeu.typ() != VorlagenTyp.BAUSTEIN) {
            return;
        }
        List<FehlendeAufloesung> fehlend = BausteinPruefung.pruefeBausteinAenderung(bausteinNeu,
                referenzierendeFormulare(bausteinNeu.alsBausteinName()), audit.jetzt());
        if (!fehlend.isEmpty()) {
            throw FachlicherFehler.konflikt("BAUSTEIN_WIRD_VERWENDET",
                    "Baustein " + bausteinNeu.vollerName() + " wird von veröffentlichten oder geplanten Formularen benötigt",
                    Map.of("verwendungen", fehlend.stream().map(FehlendeAufloesung::alsDetail).toList()));
        }
    }

    /** Prüft nach dem Löschen einer Bausteinversion, dass keine Referenz ins Leere zeigt. */
    public void pruefeKeineVerwaistenReferenzen(Vorlage bausteinNeu) {
        if (bausteinNeu.typ() != VorlagenTyp.BAUSTEIN) {
            return;
        }
        List<String> verwaist = BausteinPruefung.verwaisteReferenzen(bausteinNeu, referenzierendeFormulare(bausteinNeu.alsBausteinName()));
        if (!verwaist.isEmpty()) {
            throw FachlicherFehler.konflikt("BAUSTEIN_WIRD_VERWENDET", "Die Version wird noch von Formularversionen referenziert",
                    Map.of("verwendungen", verwaist));
        }
    }
}
