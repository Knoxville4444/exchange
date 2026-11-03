package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Eine Vorlage (Formular, Baustein oder Merkblatt) mit allen Versionen.
 * <p>
 * Unveränderlich; Änderungen erzeugen über die {@code mit…}-Methoden eine neue Instanz. Die Regeln dafür
 * stehen in den Services.
 *
 * @param veroeffentlichung Zeitraum der gesamten Vorlage; {@code null} = unbeschränkt
 * @param versionen         absteigend nach Versionsnummer sortiert
 */
public record Vorlage(String kategorie, String name, VorlagenTyp typ, String bezeichnung, String beschreibung, Set<String> tags,
        Zeitraum veroeffentlichung, Audit audit, List<Version> versionen) {

    public static final int MAX_TAGS = 50;

    public Vorlage {
        Bezeichner.pruefeKategorie(kategorie);
        Bezeichner.pruefeName(name);
        Objects.requireNonNull(typ, "typ");
        Objects.requireNonNull(audit, "audit");
        if (bezeichnung == null || bezeichnung.isBlank() || bezeichnung.length() > 255) {
            throw FachlicherFehler.ungueltig("BEZEICHNUNG_UNGUELTIG", "Bezeichnung ist Pflicht (max. 255 Zeichen)");
        }
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        if (tags.size() > MAX_TAGS || tags.stream().anyMatch(t -> t.isBlank() || t.length() > 100)) {
            throw FachlicherFehler.ungueltig("TAGS_UNGUELTIG", "Höchstens " + MAX_TAGS + " Tags mit je 1–100 Zeichen");
        }
        versionen = versionen == null ? List.of()
                : versionen.stream().sorted(Comparator.comparing(Version::nummer).reversed()).toList();
    }

    public String vollerName() {
        return kategorie + "/" + name;
    }

    public BausteinName alsBausteinName() {
        return new BausteinName(kategorie, name);
    }

    // --- Versionen ----------------------------------------------------------------------------------------------

    public Optional<Version> findeVersion(VersionsNummer nummer) {
        return versionen.stream().filter(v -> v.nummer().equals(nummer)).findFirst();
    }

    public Version version(VersionsNummer nummer) {
        return findeVersion(nummer).orElseThrow(() -> FachlicherFehler.nichtGefunden("VERSION_NICHT_GEFUNDEN",
                "Version " + nummer + " von " + vollerName() + " nicht gefunden"));
    }

    public OptionalInt hoechsteHauptversion() {
        return versionen.stream().mapToInt(v -> v.nummer().haupt()).max();
    }

    public Optional<Version> hoechsteVersion() {
        return versionen.stream().findFirst();
    }

    /** Gibt es mindestens eine Version, auf die die Angabe passt (unabhängig vom Status)? */
    public boolean hatPassendeVersion(VersionsAngabe angabe) {
        return versionen.stream().anyMatch(v -> angabe.passtZu(v.nummer()));
    }

    /** Höchste Version, auf die die Angabe passt, unabhängig vom Status (Vorschau mit Entwürfen). */
    public Optional<Version> hoechstePassendeVersion(VersionsAngabe angabe) {
        return versionen.stream().filter(v -> angabe.passtZu(v.nummer())).findFirst();
    }

    /** Schema, das für eine Hauptversion gilt. */
    public Optional<InhaltRef> schemaDerHauptversion(int haupt) {
        return versionen.stream().filter(v -> v.nummer().haupt() == haupt).map(Version::schema).filter(Objects::nonNull).findFirst();
    }

    // --- Status und Auflösung -----------------------------------------------------------------------------------

    /** Ist die Vorlage als Ganzes zum Zeitpunkt nutzbar (Vorlagen-Zeitraum)? */
    public boolean istNutzbar(Instant zeitpunkt) {
        return veroeffentlichung == null || veroeffentlichung.enthaelt(zeitpunkt);
    }

    /**
     * Versionsauflösung: höchste zum Zeitpunkt veröffentlichte Version, die zur Angabe passt. Zurückgezogene Versionen
     * fallen heraus, damit greift automatisch die nächstniedrigere.
     *
     * @param angabe {@code null} = keine Einschränkung
     */
    public Optional<Version> loese(VersionsAngabe angabe, Instant zeitpunkt) {
        if (!istNutzbar(zeitpunkt)) {
            return Optional.empty();
        }
        return versionen.stream()
                .filter(v -> v.istVeroeffentlicht(zeitpunkt))
                .filter(v -> angabe == null || angabe.passtZu(v.nummer()))
                .findFirst();
    }

    public EffektiverVorlagenStatus effektiverStatus(Instant zeitpunkt) {
        if (versionen.stream().allMatch(Version::istDraft)) {
            return EffektiverVorlagenStatus.ENTWURF;
        }
        if (loese(null, zeitpunkt).isPresent()) {
            return EffektiverVorlagenStatus.VEROEFFENTLICHT;
        }
        boolean vorlageBeendet = veroeffentlichung != null && veroeffentlichung.istBeendet(zeitpunkt);
        boolean versionKommtNoch = versionen.stream()
                .anyMatch(v -> v.effektiverStatus(zeitpunkt).istAktivOderGeplant());
        return !vorlageBeendet && versionKommtNoch ? EffektiverVorlagenStatus.GEPLANT : EffektiverVorlagenStatus.ZURUECKGEZOGEN;
    }

    /** Nur löschbar, solange nie eine Version freigegeben wurde. */
    public boolean istLoeschbar() {
        return versionen.stream().allMatch(Version::istDraft);
    }

    // --- Änderungen (neue Instanz) ------------------------------------------------------------------------------

    public Vorlage mitStammdaten(String neueBezeichnung, String neueBeschreibung, Set<String> neueTags, Audit neuesAudit) {
        return new Vorlage(kategorie, name, typ, neueBezeichnung, neueBeschreibung, neueTags, veroeffentlichung, neuesAudit, versionen);
    }

    public Vorlage mitZeitraum(Zeitraum neuerZeitraum, Audit neuesAudit) {
        return new Vorlage(kategorie, name, typ, bezeichnung, beschreibung, tags, neuerZeitraum, neuesAudit, versionen);
    }

    /** Ersetzt die Version mit derselben Nummer bzw. fügt sie hinzu. */
    public Vorlage mitVersion(Version version) {
        List<Version> neu = new ArrayList<>(versionen.stream().filter(v -> !v.nummer().equals(version.nummer())).toList());
        neu.add(version);
        return new Vorlage(kategorie, name, typ, bezeichnung, beschreibung, tags, veroeffentlichung, audit, neu);
    }

    public Vorlage ohneVersion(VersionsNummer nummer) {
        return new Vorlage(kategorie, name, typ, bezeichnung, beschreibung, tags, veroeffentlichung, audit,
                versionen.stream().filter(v -> !v.nummer().equals(nummer)).toList());
    }
}
