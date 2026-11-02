package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Eine Version einer Vorlage: Inhalt (ODT bzw. PDF), bei Formularen zusätzlich Schema und
 * Baustein-Referenzen, sowie Status und Veröffentlichungszeitraum.
 * <p>
 * Änderungen erfolgen ausschließlich über das Aggregat {@link Vorlage}, das die Regeln durchsetzt.
 */
public final class Version {

    private final Long id;
    private final VersionsNummer nummer;
    private VersionStatus status;
    private Zeitraum zeitraum;
    private final String kommentar;
    private InhaltRef inhalt;
    private InhaltRef schema;
    private List<BausteinReferenz> bausteine;
    private Audit audit;

    Version(Long id, VersionsNummer nummer, VersionStatus status, Zeitraum zeitraum, String kommentar,
            InhaltRef inhalt, InhaltRef schema, List<BausteinReferenz> bausteine, Audit audit) {
        this.id = id;
        this.nummer = Objects.requireNonNull(nummer, "nummer");
        this.status = Objects.requireNonNull(status, "status");
        this.zeitraum = zeitraum;
        this.kommentar = kommentar;
        this.inhalt = Objects.requireNonNull(inhalt, "inhalt");
        this.schema = schema;
        this.bausteine = List.copyOf(bausteine == null ? List.of() : bausteine);
        this.audit = Objects.requireNonNull(audit, "audit");
        if (status == VersionStatus.FREIGEGEBEN && (zeitraum == null || zeitraum.ab() == null)) {
            throw new IllegalStateException("Freigegebene Version " + nummer + " ohne Veröffentlichungsbeginn");
        }
    }

    /** Stellt eine Version aus der Persistenz wieder her. */
    public static Version rekonstruiere(Long id, VersionsNummer nummer, VersionStatus status, Zeitraum zeitraum,
                                        String kommentar, InhaltRef inhalt, InhaltRef schema,
                                        List<BausteinReferenz> bausteine, Audit audit) {
        return new Version(id, nummer, status, zeitraum, kommentar, inhalt, schema, bausteine, audit);
    }

    public EffektiverVersionStatus effektiverStatus(Instant zeitpunkt) {
        if (status == VersionStatus.DRAFT) {
            return EffektiverVersionStatus.DRAFT;
        }
        if (!zeitraum.hatBegonnen(zeitpunkt)) {
            return EffektiverVersionStatus.GEPLANT;
        }
        return zeitraum.istBeendet(zeitpunkt) ? EffektiverVersionStatus.ZURUECKGEZOGEN : EffektiverVersionStatus.VEROEFFENTLICHT;
    }

    public boolean istVeroeffentlicht(Instant zeitpunkt) {
        return effektiverStatus(zeitpunkt) == EffektiverVersionStatus.VEROEFFENTLICHT;
    }

    public boolean istDraft() {
        return status == VersionStatus.DRAFT;
    }

    public Optional<BausteinReferenz> referenzAuf(BausteinName baustein) {
        return bausteine.stream().filter(r -> r.baustein().equals(baustein)).findFirst();
    }

    // --- nur für das Aggregat -------------------------------------------------------------

    void setzeInhalt(InhaltRef neuerInhalt, List<BausteinReferenz> neueBausteine, Audit neuesAudit) {
        this.inhalt = Objects.requireNonNull(neuerInhalt);
        this.bausteine = List.copyOf(neueBausteine);
        this.audit = neuesAudit;
    }

    void setzeSchema(InhaltRef neuesSchema, Audit neuesAudit) {
        this.schema = neuesSchema;
        this.audit = neuesAudit;
    }

    void setzeBausteine(List<BausteinReferenz> neueBausteine, Audit neuesAudit) {
        this.bausteine = List.copyOf(neueBausteine);
        this.audit = neuesAudit;
    }

    void freigeben(Zeitraum neuerZeitraum, Audit neuesAudit) {
        this.status = VersionStatus.FREIGEGEBEN;
        this.zeitraum = Objects.requireNonNull(neuerZeitraum);
        this.audit = neuesAudit;
    }

    void zurueckAufDraft(Audit neuesAudit) {
        this.status = VersionStatus.DRAFT;
        this.zeitraum = null;
        this.audit = neuesAudit;
    }

    // --- Zugriff --------------------------------------------------------------------------

    public Long id() {
        return id;
    }

    public VersionsNummer nummer() {
        return nummer;
    }

    public VersionStatus status() {
        return status;
    }

    /** Veröffentlichungszeitraum; {@code null} im Status {@code DRAFT}. */
    public Zeitraum zeitraum() {
        return zeitraum;
    }

    public String kommentar() {
        return kommentar;
    }

    public InhaltRef inhalt() {
        return inhalt;
    }

    /** XML-Schema; nur bei Formularen gesetzt. */
    public InhaltRef schema() {
        return schema;
    }

    public List<BausteinReferenz> bausteine() {
        return bausteine;
    }

    public Audit audit() {
        return audit;
    }

    @Override
    public String toString() {
        return "Version[" + nummer + ", " + status + ", " + zeitraum + "]";
    }
}
