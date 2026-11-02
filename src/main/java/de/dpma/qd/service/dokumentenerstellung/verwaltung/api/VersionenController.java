package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.AufgeloesteBausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.BausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VersionDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.ZeitraumDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionArt;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.HochgeladeneDatei;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VersionService;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VersionsAntrag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Skizze: Versionen einer Vorlage.
 * <p>
 * Nicht enthalten: {@code POST …/versionen/{version}/vorschau} – die Vorschau gehört zur Erzeugung und nutzt dort
 * {@code VorlagenAufloesung.loeseFuerVorschau(…)}.
 */
@RestController
@RequestMapping(Http.BASIS + "/vorlagen/{kategorie}/{vorlagenName}/versionen")
@PreAuthorize("hasAuthority('SCOPE_formular-verwaltung')")
class VersionenController {

    private final VersionService versionen;

    VersionenController(VersionService versionen) {
        this.versionen = versionen;
    }

    @GetMapping
    List<VersionDto> listVersionen(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return versionen.liste(kategorie, vorlagenName, stichtag).stream().map(ApiMapper::dto).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<VersionDto> createVersion(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                             @RequestParam VersionArt art,
                                             @RequestParam(required = false) Integer basisHauptversion,
                                             @RequestParam(required = false) String basisVersion,
                                             @RequestParam(required = false) String kommentar,
                                             @RequestPart(name = "inhalt", required = false) MultipartFile inhalt,
                                             @RequestPart(name = "schema", required = false) MultipartFile schema) {
        VersionDto dto = ApiMapper.dto(versionen.legeAn(kategorie, vorlagenName, new VersionsAntrag(art, basisHauptversion,
                basisVersion == null ? null : VersionsNummer.parse(basisVersion), kommentar, Http.datei(inhalt), Http.datei(schema))));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{version}")
                .buildAndExpand(dto.version()).toUri()).body(dto);
    }

    @GetMapping("/{version}")
    VersionDto getVersion(@PathVariable String kategorie, @PathVariable String vorlagenName, @PathVariable String version,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return ApiMapper.dto(versionen.lies(kategorie, vorlagenName, VersionsNummer.parse(version), stichtag));
    }

    @DeleteMapping("/{version}")
    ResponseEntity<Void> deleteVersion(@PathVariable String kategorie, @PathVariable String vorlagenName, @PathVariable String version) {
        versionen.loesche(kategorie, vorlagenName, VersionsNummer.parse(version));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{version}/inhalt")
    ResponseEntity<byte[]> getInhalt(@PathVariable String kategorie, @PathVariable String vorlagenName, @PathVariable String version) {
        return Http.binaer(versionen.inhalt(kategorie, vorlagenName, VersionsNummer.parse(version)));
    }

    @PutMapping(path = "/{version}/inhalt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VersionDto updateInhalt(@PathVariable String kategorie, @PathVariable String vorlagenName, @PathVariable String version,
                            @RequestPart("inhalt") MultipartFile inhalt) {
        return ApiMapper.dto(versionen.ersetzeInhalt(kategorie, vorlagenName, VersionsNummer.parse(version), Http.datei(inhalt)));
    }

    @GetMapping(path = "/{version}/schema", produces = MediaType.APPLICATION_XML_VALUE)
    byte[] getSchema(@PathVariable String kategorie, @PathVariable String vorlagenName, @PathVariable String version) {
        return versionen.schema(kategorie, vorlagenName, VersionsNummer.parse(version));
    }

    @PutMapping(path = "/{version}/schema", consumes = MediaType.APPLICATION_XML_VALUE)
    VersionDto updateSchema(@PathVariable String kategorie, @PathVariable String vorlagenName, @PathVariable String version,
                            @RequestBody byte[] schema) {
        return ApiMapper.dto(versionen.ersetzeSchema(kategorie, vorlagenName, VersionsNummer.parse(version),
                new HochgeladeneDatei("schema.xsd", schema)));
    }

    @GetMapping("/{version}/bausteine")
    List<AufgeloesteBausteinReferenzDto> getBausteinReferenzen(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                                               @PathVariable String version,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return ApiMapper.dto(versionen.bausteine(kategorie, vorlagenName, VersionsNummer.parse(version), stichtag));
    }

    @PutMapping("/{version}/bausteine")
    VersionDto updateBausteinReferenzen(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                        @PathVariable String version, @RequestBody List<BausteinReferenzDto> referenzen) {
        List<BausteinReferenz> refs = referenzen.stream()
                .map(r -> new BausteinReferenz(BausteinName.parse(r.baustein()), VersionsAngabe.parse(r.version())))
                .toList();
        return ApiMapper.dto(versionen.setzeBausteine(kategorie, vorlagenName, VersionsNummer.parse(version), refs));
    }

    @PutMapping("/{version}/veroeffentlichung")
    VersionDto setVersionVeroeffentlichung(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                           @PathVariable String version, @RequestBody ZeitraumDto zeitraum) {
        return ApiMapper.dto(versionen.setzeVeroeffentlichung(kategorie, vorlagenName, VersionsNummer.parse(version),
                zeitraum.ab(), zeitraum.bis()));
    }

    @DeleteMapping("/{version}/veroeffentlichung")
    VersionDto deleteVersionVeroeffentlichung(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                              @PathVariable String version) {
        return ApiMapper.dto(versionen.sageVeroeffentlichungAb(kategorie, vorlagenName, VersionsNummer.parse(version)));
    }
}
