package de.dpma.qd.service.dokumentenerstellung.impl;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Prüfungen zwischen Formularen und Bausteinen.
 * <p>
 * Kernidee: Eine Formularversion ist ab {@code max(ab, jetzt)} bis {@code bis} nutzbar (eingeschränkt durch
 * den Formular-Zeitraum). In diesem Intervall muss jeder referenzierte Baustein zu <em>jedem</em> Zeitpunkt
 * auflösbar sein. Da sich das Auflösungsergebnis nur an den Grenzen der Baustein-Zeiträume ändert, genügt es,
 * den Intervallbeginn und alle Grenzen innerhalb des Intervalls zu prüfen.
 */
public final class BausteinPruefung {

    private BausteinPruefung() {
    }

    /** Eine Stelle, an der ein Baustein nicht aufgelöst werden kann. */
    public record FehlendeAufloesung(String formular, VersionsNummer formularVersion, BausteinName baustein,
                                     VersionsAngabe angabe, Instant zeitpunkt) {

        public Map<String, Object> alsDetail() {
            return Map.of("formular", formular, "version", formularVersion.toString(),
                    "baustein", baustein.toString(), "angabe", angabe.toString(), "zeitpunkt", zeitpunkt.toString());
        }
    }

    /**
     * Prüft, ob alle Bausteine einer (freigegebenen) Formularversion während ihrer künftigen Nutzungszeit
     * auflösbar sind. Für DRAFT-Versionen und zurückgezogene Versionen gibt es nichts zu prüfen.
     *
     * @param bausteine liefert die Bausteinvorlage zu einem Namen
     */
    public static List<FehlendeAufloesung> pruefeFormularVersion(Vorlage formular, Version version,
                                                                 Function<BausteinName, Optional<Vorlage>> bausteine,
                                                                 Instant jetzt) {
        return pruefe(formular, version, version.bausteine(), bausteine, jetzt);
    }

    /**
     * Prüft die Auswirkung einer geänderten Bausteinvorlage auf alle Formulare, die ihn referenzieren.
     *
     * @param bausteinNeu Baustein im geänderten (noch nicht gespeicherten) Zustand
     * @param formulare   Formulare mit mindestens einer Version, die den Baustein referenziert
     */
    public static List<FehlendeAufloesung> pruefeBausteinAenderung(Vorlage bausteinNeu, Collection<Vorlage> formulare,
                                                                   Instant jetzt) {
        BausteinName name = bausteinNeu.alsBausteinName();
        Function<BausteinName, Optional<Vorlage>> quelle = n -> n.equals(name) ? Optional.of(bausteinNeu) : Optional.empty();
        List<FehlendeAufloesung> ergebnis = new ArrayList<>();
        for (Vorlage formular : formulare) {
            for (Version version : formular.versionen()) {
                version.referenzAuf(name).ifPresent(ref ->
                        ergebnis.addAll(pruefe(formular, version, List.of(ref), quelle, jetzt)));
            }
        }
        return ergebnis;
    }

    /**
     * Prüft, ob nach dem Löschen einer (DRAFT-)Bausteinversion jede Referenz – unabhängig vom Status der
     * Formularversion – noch auf eine existierende Version zeigt.
     */
    public static List<String> verwaisteReferenzen(Vorlage bausteinNeu, Collection<Vorlage> formulare) {
        BausteinName name = bausteinNeu.alsBausteinName();
        return formulare.stream()
                .flatMap(f -> f.versionen().stream()
                        .flatMap(v -> v.referenzAuf(name).stream()
                                .filter(ref -> !bausteinNeu.hatPassendeVersion(ref.angabe()))
                                .map(ref -> f.vollerName() + " " + v.nummer() + " → " + name + "@" + ref.angabe())))
                .toList();
    }

    /**
     * Gleicht die im ODT gefundenen Baustein-Bereiche mit den bisherigen Referenzen ab: Vorhandene Referenzen
     * bleiben, entfallene werden entfernt, neue verweisen auf die höchste existierende Hauptversion.
     *
     * @param bereiche   im ODT gefundene Bausteine
     * @param bisher     bisherige Referenzen (z. B. aus der Basisversion)
     * @param bausteine  liefert die Bausteinvorlage zu einem Namen
     */
    public static List<BausteinReferenz> gleicheAb(Set<BausteinName> bereiche, List<BausteinReferenz> bisher,
                                                   Function<BausteinName, Optional<Vorlage>> bausteine) {
        List<BausteinReferenz> ergebnis = new ArrayList<>();
        for (BausteinName name : new TreeSet<>(bereiche)) {
            Optional<BausteinReferenz> vorhanden = bisher.stream().filter(r -> r.baustein().equals(name)).findFirst();
            if (vorhanden.isPresent()) {
                ergebnis.add(vorhanden.get());
                continue;
            }
            Vorlage baustein = pruefeBaustein(name, bausteine);
            int haupt = baustein.hoechsteHauptversion().orElseThrow(() -> FachlicherFehler.ungueltig(
                    "BAUSTEIN_OHNE_VERSION", "Baustein " + name + " hat keine Version"));
            ergebnis.add(new BausteinReferenz(name, VersionsAngabe.hauptversion(haupt)));
        }
        return ergebnis;
    }

    /**
     * Prüft explizit gesetzte Referenzen: Sie müssen exakt den Bereichen im ODT entsprechen, auf
     * existierende Bausteine zeigen und eine existierende Version treffen.
     */
    public static List<BausteinReferenz> pruefeReferenzen(Set<BausteinName> bereiche, List<BausteinReferenz> referenzen,
                                                          Function<BausteinName, Optional<Vorlage>> bausteine) {
        Set<BausteinName> referenziert = new TreeSet<>();
        for (BausteinReferenz ref : referenzen) {
            if (!referenziert.add(ref.baustein())) {
                throw FachlicherFehler.ungueltig("BAUSTEIN_DOPPELT", "Baustein " + ref.baustein() + " ist doppelt referenziert");
            }
        }
        if (!referenziert.equals(new TreeSet<>(bereiche))) {
            throw FachlicherFehler.ungueltig("BAUSTEINE_PASSEN_NICHT",
                    "Referenzen müssen exakt den Baustein-Bereichen im ODT entsprechen",
                    Map.of("bereiche", bereiche.stream().sorted().map(BausteinName::toString).toList(),
                            "referenzen", referenziert.stream().map(BausteinName::toString).toList()));
        }
        for (BausteinReferenz ref : referenzen) {
            Vorlage baustein = pruefeBaustein(ref.baustein(), bausteine);
            if (!baustein.hatPassendeVersion(ref.angabe())) {
                throw FachlicherFehler.ungueltig("BAUSTEINVERSION_UNBEKANNT",
                        "Baustein " + ref.baustein() + " hat keine Version " + ref.angabe());
            }
        }
        return referenzen.stream().sorted(Comparator.comparing(BausteinReferenz::baustein)).toList();
    }

    // -------------------------------------------------------------------------------------

    private static Vorlage pruefeBaustein(BausteinName name, Function<BausteinName, Optional<Vorlage>> bausteine) {
        Vorlage baustein = bausteine.apply(name).orElseThrow(() -> FachlicherFehler.ungueltig(
                "BAUSTEIN_UNBEKANNT", "Bereich '" + name.bereichsname() + "' verweist auf keinen existierenden Baustein"));
        if (baustein.typ() != VorlagenTyp.BAUSTEIN) {
            throw FachlicherFehler.ungueltig("KEIN_BAUSTEIN", name + " ist vom Typ " + baustein.typ());
        }
        return baustein;
    }

    private static List<FehlendeAufloesung> pruefe(Vorlage formular, Version version, List<BausteinReferenz> referenzen,
                                                   Function<BausteinName, Optional<Vorlage>> bausteine, Instant jetzt) {
        if (version.status() != VersionStatus.FREIGEGEBEN) {
            return List.of();
        }
        Instant start = max(version.zeitraum().ab(), jetzt);
        Instant ende = version.zeitraum().bis();
        Zeitraum formularZeitraum = formular.veroeffentlichung();
        if (formularZeitraum != null) {
            start = max(start, formularZeitraum.ab());
            ende = min(ende, formularZeitraum.bis());
        }
        if (ende != null && !start.isBefore(ende)) {
            return List.of();
        }
        List<FehlendeAufloesung> ergebnis = new ArrayList<>();
        for (BausteinReferenz ref : referenzen) {
            Optional<Vorlage> baustein = bausteine.apply(ref.baustein());
            if (baustein.isEmpty()) {
                ergebnis.add(new FehlendeAufloesung(formular.vollerName(), version.nummer(), ref.baustein(), ref.angabe(), start));
                continue;
            }
            for (Instant zeitpunkt : pruefzeitpunkte(baustein.get(), start, ende)) {
                if (baustein.get().loese(ref.angabe(), zeitpunkt).isEmpty()) {
                    ergebnis.add(new FehlendeAufloesung(formular.vollerName(), version.nummer(), ref.baustein(), ref.angabe(), zeitpunkt));
                    break;
                }
            }
        }
        return ergebnis;
    }

    private static TreeSet<Instant> pruefzeitpunkte(Vorlage baustein, Instant start, Instant ende) {
        TreeSet<Instant> punkte = new TreeSet<>();
        punkte.add(start);
        Stream<Zeitraum> zeitraeume = Stream.concat(
                baustein.versionen().stream().filter(v -> v.status() == VersionStatus.FREIGEGEBEN).map(Version::zeitraum),
                Stream.ofNullable(baustein.veroeffentlichung()));
        zeitraeume.flatMap(z -> Stream.of(z.ab(), z.bis()))
                .filter(p -> p != null && p.isAfter(start) && (ende == null || p.isBefore(ende)))
                .forEach(punkte::add);
        return punkte;
    }

    private static Instant max(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }
}
