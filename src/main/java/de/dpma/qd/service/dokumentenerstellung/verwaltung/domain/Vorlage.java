package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Aggregat einer Vorlage (Formular, Baustein oder Merkblatt) mit allen Versionen.
 * <p>
 * Hier liegen die Regeln für Nummernvergabe, Änderbarkeit, Veröffentlichung und Versionsauflösung.
 * Prüfungen, die andere Vorlagen betreffen (Bausteine), übernimmt {@link BausteinPruefung}.
 */
public final class Vorlage {

    public static final int MAX_TAGS = 50;

    private final Long id;
    private final String kategorie;
    private final String name;
    private final VorlagenTyp typ;
    private String bezeichnung;
    private String beschreibung;
    private Set<String> tags;
    private Zeitraum veroeffentlichung;
    private Audit audit;
    private final Long revision;
    private final List<Version> versionen;

    private Vorlage(Long id, String kategorie, String name, VorlagenTyp typ, String bezeichnung, String beschreibung,
                    Set<String> tags, Zeitraum veroeffentlichung, Audit audit, Long revision, List<Version> versionen) {
        this.id = id;
        this.kategorie = Bezeichner.pruefeKategorie(kategorie);
        this.name = Bezeichner.pruefeName(name);
        this.typ = Objects.requireNonNull(typ, "typ");
        this.bezeichnung = pruefeBezeichnung(bezeichnung);
        this.beschreibung = beschreibung;
        this.tags = pruefeTags(tags);
        this.veroeffentlichung = veroeffentlichung;
        this.audit = Objects.requireNonNull(audit, "audit");
        this.revision = revision;
        this.versionen = new ArrayList<>(versionen);
    }

    /** Legt eine neue Vorlage mit Version 1.0 im Status DRAFT an. */
    public static Vorlage neu(String kategorie, String name, VorlagenTyp typ, String bezeichnung, String beschreibung,
                              Set<String> tags, String kommentar, InhaltRef inhalt, InhaltRef schema,
                              List<BausteinReferenz> bausteine, Audit audit) {
        Vorlage vorlage = new Vorlage(null, kategorie, name, typ, bezeichnung, beschreibung, tags, null, audit, null, List.of());
        vorlage.pruefeVersionsinhalt(VersionsNummer.erste(), inhalt, schema, bausteine);
        vorlage.versionen.add(new Version(null, VersionsNummer.erste(), VersionStatus.DRAFT, null, kommentar,
                inhalt, schema, bausteine, audit));
        return vorlage;
    }

    /** Stellt eine Vorlage aus der Persistenz wieder her. */
    public static Vorlage rekonstruiere(Long id, String kategorie, String name, VorlagenTyp typ, String bezeichnung,
                                        String beschreibung, Set<String> tags, Zeitraum veroeffentlichung, Audit audit,
                                        Long revision, List<Version> versionen) {
        return new Vorlage(id, kategorie, name, typ, bezeichnung, beschreibung, tags, veroeffentlichung, audit,
                revision, versionen);
    }

    // =====================================================================================
    // Lesen
    // =====================================================================================

    /** Alle Versionen, absteigend nach Versionsnummer. */
    public List<Version> versionen() {
        return versionen.stream().sorted(Comparator.comparing(Version::nummer).reversed()).toList();
    }

    public Optional<Version> findeVersion(VersionsNummer nummer) {
        return versionen.stream().filter(v -> v.nummer().equals(nummer)).findFirst();
    }

    public Version version(VersionsNummer nummer) {
        return findeVersion(nummer).orElseThrow(() -> new NichtGefundenException("VERSION_NICHT_GEFUNDEN",
                "Version " + nummer + " von " + vollerName() + " nicht gefunden"));
    }

    public OptionalInt hoechsteHauptversion() {
        return versionen.stream().mapToInt(v -> v.nummer().haupt()).max();
    }

    /** Gibt es mindestens eine Version, auf die die Angabe passt (unabhängig vom Status)? */
    public boolean hatPassendeVersion(VersionsAngabe angabe) {
        return versionen.stream().anyMatch(v -> angabe.passtZu(v.nummer()));
    }

    /** Höchste Version, auf die die Angabe passt – unabhängig vom Status (für die Vorschau mit Entwürfen). */
    public Optional<Version> hoechstePassendeVersion(VersionsAngabe angabe) {
        return versionen.stream().filter(v -> angabe.passtZu(v.nummer())).max(Comparator.comparing(Version::nummer));
    }

    /** Ist die Vorlage als Ganzes zum Zeitpunkt nutzbar (Vorlagen-Zeitraum)? */
    public boolean istNutzbar(Instant zeitpunkt) {
        return veroeffentlichung == null || veroeffentlichung.enthaelt(zeitpunkt);
    }

    /**
     * Versionsauflösung: höchste zum Zeitpunkt veröffentlichte Version, die zur Angabe passt.
     * Zurückgezogene Versionen fallen heraus, damit greift automatisch die nächstniedrigere.
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
                .max(Comparator.comparing(Version::nummer));
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
                .map(v -> v.effektiverStatus(zeitpunkt))
                .anyMatch(EffektiverVersionStatus::istAktivOderGeplant);
        return !vorlageBeendet && versionKommtNoch ? EffektiverVorlagenStatus.GEPLANT : EffektiverVorlagenStatus.ZURUECKGEZOGEN;
    }

    /** Nur löschbar, solange nie eine Version freigegeben wurde. */
    public boolean istLoeschbar() {
        return versionen.stream().allMatch(Version::istDraft);
    }

    public String vollerName() {
        return kategorie + "/" + name;
    }

    public BausteinName alsBausteinName() {
        if (typ != VorlagenTyp.BAUSTEIN) {
            throw new IllegalStateException(vollerName() + " ist kein Baustein");
        }
        return new BausteinName(kategorie, name);
    }

    // =====================================================================================
    // Stammdaten
    // =====================================================================================

    public void aendereStammdaten(String neueBezeichnung, String neueBeschreibung, Set<String> neueTags, Audit neuesAudit) {
        this.bezeichnung = pruefeBezeichnung(neueBezeichnung);
        this.beschreibung = neueBeschreibung;
        this.tags = pruefeTags(neueTags);
        this.audit = neuesAudit;
    }

    // =====================================================================================
    // Versionen anlegen und ändern
    // =====================================================================================

    /** Ergebnis der Planung einer neuen Version: vergebene Nummer und Kopiervorlage. */
    public record VersionsPlan(VersionsNummer nummer, Optional<Version> basis) {
    }

    /**
     * Ermittelt die nächste Versionsnummer und die Version, aus der Inhalt, Schema und Bausteine
     * übernommen werden.
     *
     * @param basisHauptversion nur bei {@code NEBEN}: Hauptversion, deren Nebenversion hochgezählt wird
     * @param basisVersion      optional: Version, aus der kopiert wird
     */
    public VersionsPlan planeNeueVersion(VersionArt art, Integer basisHauptversion, VersionsNummer basisVersion) {
        Optional<Version> basis = basisVersion == null ? Optional.empty() : Optional.of(version(basisVersion));
        if (art == VersionArt.HAUPT) {
            if (basisHauptversion != null) {
                throw new UngueltigeAnfrageException("BASIS_HAUPTVERSION_NICHT_ZULAESSIG",
                        "basisHauptversion ist nur bei art=NEBEN zulässig");
            }
            VersionsNummer nummer = new VersionsNummer(hoechsteHauptversion().orElse(0) + 1, 0);
            return new VersionsPlan(nummer, basis.or(this::hoechsteVersion));
        }
        int haupt = basisHauptversion != null ? basisHauptversion
                : hoechsteHauptversion().orElseThrow(() -> new UngueltigeAnfrageException("KEINE_HAUPTVERSION",
                "Für " + vollerName() + " existiert keine Hauptversion; zuerst art=HAUPT anlegen"));
        VersionsNummer hoechsteInHaupt = versionen.stream().map(Version::nummer)
                .filter(n -> n.haupt() == haupt).max(Comparator.naturalOrder())
                .orElseThrow(() -> new NichtGefundenException("HAUPTVERSION_NICHT_GEFUNDEN",
                        "Hauptversion " + haupt + " von " + vollerName() + " existiert nicht"));
        if (basis.isPresent() && basis.get().nummer().haupt() != haupt) {
            throw new UngueltigeAnfrageException("BASIS_VERSION_PASST_NICHT",
                    "basisVersion " + basisVersion + " gehört nicht zur Hauptversion " + haupt);
        }
        return new VersionsPlan(hoechsteInHaupt.naechsteNebenversion(), basis.or(() -> findeVersion(hoechsteInHaupt)));
    }

    /** Fügt eine geplante Version im Status DRAFT hinzu. */
    public Version fuegeVersionHinzu(VersionsNummer nummer, String kommentar, InhaltRef inhalt, InhaltRef schema,
                                     List<BausteinReferenz> bausteine, Audit neuesAudit) {
        if (findeVersion(nummer).isPresent()) {
            throw new KonfliktException("VERSION_EXISTIERT", "Version " + nummer + " existiert bereits");
        }
        pruefeVersionsinhalt(nummer, inhalt, schema, bausteine);
        Version version = new Version(null, nummer, VersionStatus.DRAFT, null, kommentar, inhalt, schema, bausteine, neuesAudit);
        versionen.add(version);
        this.audit = neuesAudit;
        return version;
    }

    public void loescheVersion(VersionsNummer nummer) {
        Version version = version(nummer);
        pruefeDraft(version);
        versionen.remove(version);
    }

    public void ersetzeInhalt(VersionsNummer nummer, InhaltRef inhalt, List<BausteinReferenz> bausteine, Audit neuesAudit) {
        Version version = version(nummer);
        pruefeDraft(version);
        pruefeVersionsinhalt(nummer, inhalt, version.schema(), bausteine);
        version.setzeInhalt(inhalt, bausteine, neuesAudit);
    }

    /**
     * Ersetzt das Schema einer Hauptversion. Das Schema gilt für alle Versionen der Hauptversion und
     * ist daher nur änderbar, solange keine davon freigegeben wurde.
     */
    public void ersetzeSchema(VersionsNummer nummer, InhaltRef schema, Audit neuesAudit) {
        Version version = version(nummer);
        if (!typ.istFormular()) {
            throw new KonfliktException("TYP_UNPASSEND", "Nur Formulare haben ein Schema");
        }
        pruefeDraft(version);
        if (!nummer.istHauptversion()) {
            throw new KonfliktException("SCHEMA_NUR_HAUPTVERSION",
                    "Das Schema wird an der Hauptversion " + nummer.haupt() + ".0 gepflegt");
        }
        List<Version> derHauptversion = versionen.stream().filter(v -> v.nummer().haupt() == nummer.haupt()).toList();
        if (derHauptversion.stream().anyMatch(v -> !v.istDraft())) {
            throw new KonfliktException("SCHEMA_IN_VERWENDUNG",
                    "Hauptversion " + nummer.haupt() + " hat bereits freigegebene Versionen; Schema ist unveränderlich");
        }
        derHauptversion.forEach(v -> v.setzeSchema(Objects.requireNonNull(schema), neuesAudit));
    }

    public void setzeBausteine(VersionsNummer nummer, List<BausteinReferenz> bausteine, Audit neuesAudit) {
        Version version = version(nummer);
        if (!typ.istFormular()) {
            throw new KonfliktException("TYP_UNPASSEND", "Nur Formulare haben Baustein-Referenzen");
        }
        pruefeDraft(version);
        version.setzeBausteine(bausteine, neuesAudit);
    }

    /** Schema, das für eine Hauptversion gilt (aus einer beliebigen Version dieser Hauptversion). */
    public Optional<InhaltRef> schemaDerHauptversion(int haupt) {
        return versionen.stream().filter(v -> v.nummer().haupt() == haupt).map(Version::schema)
                .filter(Objects::nonNull).findFirst();
    }

    // =====================================================================================
    // Veröffentlichung
    // =====================================================================================

    /**
     * Veröffentlicht eine Version (sofort oder geplant), verschiebt den Termin oder zieht sie zurück.
     *
     * @param ab  {@code null} = jetzt bzw. unverändert bei bereits veröffentlichten Versionen
     * @param bis {@code null} = unbefristet
     */
    public Version setzeVeroeffentlichung(VersionsNummer nummer, Instant ab, Instant bis, Instant jetzt, Audit neuesAudit) {
        Version version = version(nummer);
        EffektiverVersionStatus status = version.effektiverStatus(jetzt);
        if (bis != null && bis.isBefore(jetzt)) {
            throw new UngueltigeAnfrageException("BIS_IN_VERGANGENHEIT", "bis darf nicht in der Vergangenheit liegen");
        }
        Zeitraum neu = switch (status) {
            case DRAFT, GEPLANT -> {
                Instant neuesAb = ab == null ? jetzt : ab;
                if (neuesAb.isBefore(jetzt)) {
                    throw new UngueltigeAnfrageException("AB_IN_VERGANGENHEIT",
                            "ab darf nicht in der Vergangenheit liegen; für sofortige Veröffentlichung ab weglassen");
                }
                yield new Zeitraum(neuesAb, bis);
            }
            case VEROEFFENTLICHT -> {
                if (ab != null && !ab.equals(version.zeitraum().ab())) {
                    throw new KonfliktException("AB_UNVERAENDERLICH",
                            "Version " + nummer + " ist veröffentlicht; ab ist nicht mehr änderbar");
                }
                yield new Zeitraum(version.zeitraum().ab(), bis);
            }
            case ZURUECKGEZOGEN -> throw new KonfliktException("VERSION_ZURUECKGEZOGEN",
                    "Version " + nummer + " ist zurückgezogen und nicht mehr änderbar");
        };
        version.freigeben(neu, neuesAudit);
        this.audit = neuesAudit;
        return version;
    }

    /** Sagt eine geplante Veröffentlichung ab; die Version wird wieder DRAFT. */
    public Version sageVeroeffentlichungAb(VersionsNummer nummer, Instant jetzt, Audit neuesAudit) {
        Version version = version(nummer);
        if (version.effektiverStatus(jetzt) != EffektiverVersionStatus.GEPLANT) {
            throw new KonfliktException("VERSION_NICHT_GEPLANT",
                    "Nur geplante Veröffentlichungen können abgesagt werden (Version " + nummer + ")");
        }
        version.zurueckAufDraft(neuesAudit);
        this.audit = neuesAudit;
        return version;
    }

    /**
     * Setzt den Zeitraum der gesamten Vorlage ({@code null} entfernt ihn). Änderungen dürfen nur in die
     * Zukunft wirken, damit frühere Zustände rekonstruierbar bleiben.
     */
    public void setzeVorlagenZeitraum(Zeitraum neu, Instant jetzt, Audit neuesAudit) {
        Zeitraum bereinigt = neu == null || (neu.ab() == null && neu.bis() == null) ? null : neu;
        if (!Zeitraum.gleicheVergangenheit(veroeffentlichung, bereinigt, jetzt)) {
            throw new KonfliktException("VERGANGENHEIT_UNVERAENDERLICH",
                    "Der Zeitraum darf nur mit Wirkung für die Zukunft geändert werden");
        }
        this.veroeffentlichung = bereinigt;
        this.audit = neuesAudit;
    }

    // =====================================================================================
    // Regeln
    // =====================================================================================

    private void pruefeDraft(Version version) {
        if (!version.istDraft()) {
            throw new KonfliktException("VERSION_NICHT_DRAFT",
                    "Version " + version.nummer() + " ist nicht im Status DRAFT und damit unveränderlich");
        }
    }

    private void pruefeVersionsinhalt(VersionsNummer nummer, InhaltRef inhalt, InhaltRef schema,
                                      List<BausteinReferenz> bausteine) {
        Objects.requireNonNull(inhalt, "inhalt");
        if (inhalt.format() != typ.inhaltsformat()) {
            throw new UngueltigeAnfrageException("INHALT_PASST_NICHT_ZUM_TYP",
                    typ + " erwartet " + typ.inhaltsformat() + ", erhalten: " + inhalt.format());
        }
        if (typ.istFormular() && schema == null) {
            throw new UngueltigeAnfrageException("SCHEMA_FEHLT", "Formulare benötigen ein XML-Schema (XSD)");
        }
        if (!typ.istFormular() && schema != null) {
            throw new UngueltigeAnfrageException("SCHEMA_NICHT_ZULAESSIG", typ + " hat kein Schema");
        }
        if (!typ.istFormular() && bausteine != null && !bausteine.isEmpty()) {
            throw new UngueltigeAnfrageException("BAUSTEINE_NICHT_ZULAESSIG", typ + " kann keine Bausteine enthalten");
        }
        if (schema != null && !nummer.istHauptversion()) {
            schemaDerHauptversion(nummer.haupt()).ifPresent(s -> {
                if (s.id() != schema.id()) {
                    throw new UngueltigeAnfrageException("SCHEMA_NICHT_ZULAESSIG",
                            "Nebenversionen übernehmen das Schema ihrer Hauptversion");
                }
            });
        }
    }

    private Optional<Version> hoechsteVersion() {
        return versionen.stream().max(Comparator.comparing(Version::nummer));
    }

    private static String pruefeBezeichnung(String bezeichnung) {
        if (bezeichnung == null || bezeichnung.isBlank() || bezeichnung.length() > 255) {
            throw new UngueltigeAnfrageException("BEZEICHNUNG_UNGUELTIG", "Bezeichnung ist Pflicht (max. 255 Zeichen)");
        }
        return bezeichnung;
    }

    private static Set<String> pruefeTags(Set<String> tags) {
        Set<String> ergebnis = new LinkedHashSet<>(tags == null ? Set.of() : tags);
        if (ergebnis.size() > MAX_TAGS || ergebnis.stream().anyMatch(t -> t == null || t.isBlank() || t.length() > 100)) {
            throw new UngueltigeAnfrageException("TAGS_UNGUELTIG", "Höchstens " + MAX_TAGS + " Tags mit je 1–100 Zeichen");
        }
        return Set.copyOf(ergebnis);
    }

    // =====================================================================================
    // Zugriff
    // =====================================================================================

    public Long id() {
        return id;
    }

    public String kategorie() {
        return kategorie;
    }

    public String name() {
        return name;
    }

    public VorlagenTyp typ() {
        return typ;
    }

    public String bezeichnung() {
        return bezeichnung;
    }

    public String beschreibung() {
        return beschreibung;
    }

    public Set<String> tags() {
        return tags;
    }

    /** Zeitraum der gesamten Vorlage; {@code null} = unbeschränkt. */
    public Zeitraum veroeffentlichung() {
        return veroeffentlichung;
    }

    public Audit audit() {
        return audit;
    }

    /** Technische Revision für optimistisches Locking; {@code null} bei neuen Vorlagen. */
    public Long revision() {
        return revision;
    }
}
