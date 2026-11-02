package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.AufgeloesteBausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.AufloesungDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.AuditDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.KategorieDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VersionDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VersionKurzDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VerwendungDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VorlageDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.ZeitraumDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Audit;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Zeitraum;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.AufgeloesteReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.KategorieAnsicht;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VersionsAnsicht;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.Verwendung;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VorlagenAnsicht;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung.AufloesungsErgebnis;

import java.util.List;

/** Abbildung Service-Ansichten → DTOs. */
final class ApiMapper {

    private ApiMapper() {
    }

    static KategorieDto dto(KategorieAnsicht a) {
        return new KategorieDto(a.kategorie().name(), a.kategorie().beschreibung(), a.anzahlVorlagen());
    }

    static VorlageDto dto(VorlagenAnsicht a) {
        Vorlage v = a.vorlage();
        return new VorlageDto(v.kategorie(), v.name(), v.typ(), v.bezeichnung(), v.beschreibung(), v.tags(),
                dto(v.veroeffentlichung()), a.stichtag(), a.effektiverStatus(),
                a.aktuelleVersion().map(VersionsNummer::toString).orElse(null), dto(v.audit()),
                a.versionen().stream().map(k -> new VersionKurzDto(k.version().toString(), k.effektiverStatus())).toList());
    }

    static VersionDto dto(VersionsAnsicht a) {
        Version v = a.version();
        return new VersionDto(a.kategorie(), a.vorlagenName(), a.typ(), v.nummer().toString(), v.nummer().haupt(),
                v.nummer().neben(), v.status(), a.effektiverStatus(), dto(v.zeitraum()), v.kommentar(),
                a.typ().istFormular() ? dto(a.bausteine()) : null, v.inhalt().sha256(), dto(v.audit()));
    }

    static List<AufgeloesteBausteinReferenzDto> dto(List<AufgeloesteReferenz> referenzen) {
        return referenzen.stream().map(r -> new AufgeloesteBausteinReferenzDto(r.baustein().toString(), r.angabe().toString(),
                r.aufgeloesteVersion().map(VersionsNummer::toString).orElse(null))).toList();
    }

    static VerwendungDto dto(Verwendung v) {
        return new VerwendungDto(v.formular(), v.version().toString(), v.effektiverStatus(), v.angabe().toString(),
                v.aufgeloesteVersion().map(VersionsNummer::toString).orElse(null));
    }

    static AufloesungDto dto(AufloesungsErgebnis e) {
        return new AufloesungDto(e.stichtag(), e.kategorie(), e.name(), e.version().toString(),
                e.typ().istFormular() ? dto(e.bausteine()) : null);
    }

    static ZeitraumDto dto(Zeitraum z) {
        return z == null ? null : new ZeitraumDto(z.ab(), z.bis());
    }

    static AuditDto dto(Audit a) {
        return new AuditDto(a.erstelltAm(), a.erstelltVon(), a.geaendertAm(), a.geaendertVon());
    }
}
