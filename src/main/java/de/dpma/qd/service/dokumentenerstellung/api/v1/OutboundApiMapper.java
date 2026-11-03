package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.AuditDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.AufgeloesteBausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.AufloesungResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.EffektiverVersionStatusDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.EffektiverVorlagenStatusDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.KategorieResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionKurzDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionStatusDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VerwendungResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlageResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlagenTypDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.ZeitraumDto;
import de.dpma.qd.service.dokumentenerstellung.impl.Audit;
import de.dpma.qd.service.dokumentenerstellung.impl.AufgeloesteReferenz;
import de.dpma.qd.service.dokumentenerstellung.impl.KategorieAnsicht;
import de.dpma.qd.service.dokumentenerstellung.impl.Version;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsAnsicht;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.impl.Verwendung;
import de.dpma.qd.service.dokumentenerstellung.impl.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenAnsicht;
import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenAufloesungService;
import de.dpma.qd.service.dokumentenerstellung.impl.Zeitraum;

/** Mapping Fachobjekte → generierte API-DTOs. Zeitpunkte werden in UTC ausgegeben. */
final class OutboundApiMapper {

    private OutboundApiMapper() {
    }

    static KategorieResponseDto dto(KategorieAnsicht a) {
        return new KategorieResponseDto(a.kategorie().name(), a.kategorie().beschreibung(), Math.toIntExact(a.anzahlVorlagen()));
    }

    static VorlageResponseDto dto(VorlagenAnsicht a) {
        Vorlage v = a.vorlage();
        List<VersionKurzDto> versionen = v.versionen().stream()
                .map(ver -> new VersionKurzDto(ver.nummer().toString(), EffektiverVersionStatusDto.valueOf(ver.effektiverStatus(a.stichtag()).name())))
                .toList();
        return new VorlageResponseDto(v.kategorie(), v.name(), VorlagenTypDto.valueOf(v.typ().name()), v.bezeichnung(), v.beschreibung(),
                new LinkedHashSet<>(v.tags().stream().sorted().toList()), dto(v.veroeffentlichung()), zeit(a.stichtag()),
                EffektiverVorlagenStatusDto.valueOf(a.effektiverStatus().name()), a.aktuelleVersion().map(VersionsNummer::toString).orElse(null),
                dto(v.audit()), new ArrayList<>(versionen));
    }

    static VersionResponseDto dto(VersionsAnsicht a) {
        Vorlage vorlage = a.vorlage();
        Version v = a.version();
        return new VersionResponseDto(vorlage.kategorie(), vorlage.name(), VorlagenTypDto.valueOf(vorlage.typ().name()), v.nummer().toString(),
                v.nummer().haupt(), v.nummer().neben(), VersionStatusDto.valueOf(v.status().name()),
                EffektiverVersionStatusDto.valueOf(a.effektiverStatus().name()), dto(v.zeitraum()), v.kommentar(), referenzen(a.bausteine()),
                v.inhalt().sha256(), dto(v.audit()));
    }

    static List<AufgeloesteBausteinReferenzDto> referenzen(List<AufgeloesteReferenz> referenzen) {
        return new ArrayList<>(referenzen.stream()
                .map(r -> new AufgeloesteBausteinReferenzDto(r.baustein().toString(), r.angabe().toString(),
                        r.aufgeloesteVersion().map(VersionsNummer::toString).orElse(null)))
                .toList());
    }

    static VerwendungResponseDto dto(Verwendung v) {
        return new VerwendungResponseDto(v.formular(), v.version().toString(), EffektiverVersionStatusDto.valueOf(v.effektiverStatus().name()),
                v.angabe().toString(), v.aufgeloesteVersion().map(VersionsNummer::toString).orElse(null));
    }

    static AufloesungResponseDto dto(VorlagenAufloesungService.Ergebnis e) {
        return new AufloesungResponseDto(zeit(e.stichtag()), e.vorlage().kategorie(), e.vorlage().name(), e.version().nummer().toString(),
                referenzen(e.bausteine()));
    }

    static ZeitraumDto dto(Zeitraum z) {
        return z == null ? null : new ZeitraumDto(zeit(z.ab()), zeit(z.bis()));
    }

    static AuditDto dto(Audit a) {
        return new AuditDto(zeit(a.erstelltAm()), a.erstelltVon(), zeit(a.geaendertAm()), a.geaendertVon());
    }

    private static OffsetDateTime zeit(Instant zeitpunkt) {
        return zeitpunkt == null ? null : zeitpunkt.atOffset(ZoneOffset.UTC);
    }
}
