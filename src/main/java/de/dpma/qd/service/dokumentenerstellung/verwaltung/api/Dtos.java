package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVersionStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.EffektiverVorlagenStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DTOs der Verwaltungs-API, benannt wie die Schemas der OpenAPI.
 * <p>
 * <b>Skizze:</b> In der Umsetzung werden sie durch die vom openapi-generator erzeugten Modelle ersetzt.
 */
public final class Dtos {

    private Dtos() {
    }

    public record ZeitraumDto(Instant ab, Instant bis) {
    }

    public record AuditDto(Instant erstelltAm, String erstelltVon, Instant geaendertAm, String geaendertVon) {
    }

    public record KategorieRequestDto(String beschreibung) {
    }

    public record KategorieDto(String kategorie, String beschreibung, long anzahlVorlagen) {
    }

    public record VersionKurzDto(String version, EffektiverVersionStatus effektiverStatus) {
    }

    public record VorlageDto(String kategorie, String vorlagenName, VorlagenTyp typ, String bezeichnung,
                             String beschreibung, Set<String> tags, ZeitraumDto veroeffentlichung, Instant stichtag,
                             EffektiverVorlagenStatus effektiverStatus, String aktuelleVersion, AuditDto audit,
                             List<VersionKurzDto> versionen) {
    }

    public record VorlageUpdateRequestDto(String bezeichnung, String beschreibung, Set<String> tags) {
    }

    public record BausteinReferenzDto(String baustein, String version) {
    }

    public record AufgeloesteBausteinReferenzDto(String baustein, String angabe, String aufgeloesteVersion) {
    }

    public record VersionDto(String kategorie, String vorlagenName, VorlagenTyp typ, String version, int hauptversion,
                             int nebenversion, VersionStatus status, EffektiverVersionStatus effektiverStatus,
                             ZeitraumDto veroeffentlichung, String kommentar,
                             List<AufgeloesteBausteinReferenzDto> bausteine, String inhaltPruefsumme, AuditDto audit) {
    }

    public record VerwendungDto(String formular, String version, EffektiverVersionStatus effektiverStatus, String angabe,
                                String aufgeloesteVersion) {
    }

    public record AufloesungDto(Instant stichtag, String kategorie, String vorlagenName, String version,
                                List<AufgeloesteBausteinReferenzDto> bausteine) {
    }

    public record RestErrorInfo(String errorId, String errorMessage, String stackTrace, String versionInfo,
                                boolean retryPossible, Map<String, Object> detail) {
    }
}
