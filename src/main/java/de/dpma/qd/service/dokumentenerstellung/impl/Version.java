package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Eine Version einer Vorlage: Inhalt (ODT bzw. PDF), bei Formularen zusätzlich Schema und Baustein-Referenzen,
 * sowie Status und Veröffentlichungszeitraum.
 *
 * @param zeitraum Veröffentlichungszeitraum; {@code null} im Status {@code DRAFT}
 * @param schema   XML-Schema; nur bei Formularen gesetzt
 */
public record Version(VersionsNummer nummer, VersionStatus status, Zeitraum zeitraum, String kommentar, InhaltRef inhalt,
        InhaltRef schema, List<BausteinReferenz> bausteine, Audit audit) {

    public Version {
        Objects.requireNonNull(nummer, "nummer");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(inhalt, "inhalt");
        Objects.requireNonNull(audit, "audit");
        bausteine = bausteine == null ? List.of() : List.copyOf(bausteine);
        if (status == VersionStatus.FREIGEGEBEN && (zeitraum == null || zeitraum.ab() == null)) {
            throw new IllegalStateException("Freigegebene Version " + nummer + " ohne Veröffentlichungsbeginn");
        }
    }

    /** Neue Version im Status DRAFT. */
    public static Version entwurf(VersionsNummer nummer, String kommentar, InhaltRef inhalt, InhaltRef schema,
            List<BausteinReferenz> bausteine, Audit audit) {
        return new Version(nummer, VersionStatus.DRAFT, null, kommentar, inhalt, schema, bausteine, audit);
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

    public Version mitInhalt(InhaltRef neuerInhalt, List<BausteinReferenz> neueBausteine, Audit neuesAudit) {
        return new Version(nummer, status, zeitraum, kommentar, neuerInhalt, schema, neueBausteine, neuesAudit);
    }

    public Version mitSchema(InhaltRef neuesSchema, Audit neuesAudit) {
        return new Version(nummer, status, zeitraum, kommentar, inhalt, neuesSchema, bausteine, neuesAudit);
    }

    public Version mitBausteinen(List<BausteinReferenz> neueBausteine, Audit neuesAudit) {
        return new Version(nummer, status, zeitraum, kommentar, inhalt, schema, neueBausteine, neuesAudit);
    }

    public Version freigegeben(Zeitraum neuerZeitraum, Audit neuesAudit) {
        return new Version(nummer, VersionStatus.FREIGEGEBEN, neuerZeitraum, kommentar, inhalt, schema, bausteine, neuesAudit);
    }

    public Version alsEntwurf(Audit neuesAudit) {
        return new Version(nummer, VersionStatus.DRAFT, null, kommentar, inhalt, schema, bausteine, neuesAudit);
    }
}
