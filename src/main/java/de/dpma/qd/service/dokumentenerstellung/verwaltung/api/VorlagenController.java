package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VerwendungDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VorlageDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.VorlageUpdateRequestDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.ZeitraumDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Zeitraum;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VorlagenAntrag;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VorlagenService;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenFilter;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Skizze: Vorlagen (Suche, Anlage, Stammdaten, Zeitraum, aufgelöster Inhalt, Verwendungsnachweis). */
@RestController
@RequestMapping(Http.BASIS + "/vorlagen")
@PreAuthorize("hasAuthority('SCOPE_formular-verwaltung')")
class VorlagenController {

    private final VorlagenService vorlagen;

    VorlagenController(VorlagenService vorlagen) {
        this.vorlagen = vorlagen;
    }

    @GetMapping
    List<VorlageDto> sucheVorlagen(@RequestParam(required = false) Set<String> kategorie,
                                   @RequestParam(required = false) Set<VorlagenTyp> typ,
                                   @RequestParam(required = false) Set<String> tag,
                                   @RequestParam(required = false) String suchtext,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return vorlagen.suche(new VorlagenFilter(kategorie, typ, tag, suchtext), stichtag).stream().map(ApiMapper::dto).toList();
    }

    @PostMapping(path = "/{kategorie}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<VorlageDto> createVorlage(@PathVariable String kategorie,
                                             @RequestParam String vorlagenName,
                                             @RequestParam VorlagenTyp typ,
                                             @RequestParam String bezeichnung,
                                             @RequestParam(required = false) String beschreibung,
                                             @RequestParam(required = false) List<String> tags,
                                             @RequestParam(required = false) String kommentar,
                                             @RequestPart("inhalt") MultipartFile inhalt,
                                             @RequestPart(name = "schema", required = false) MultipartFile schema) {
        VorlageDto dto = ApiMapper.dto(vorlagen.legeAn(kategorie, new VorlagenAntrag(vorlagenName, typ, bezeichnung,
                beschreibung, tags == null ? Set.of() : new HashSet<>(tags), kommentar, Http.datei(inhalt), Http.datei(schema))));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{name}")
                .buildAndExpand(vorlagenName).toUri()).body(dto);
    }

    @GetMapping("/{kategorie}/{vorlagenName}")
    VorlageDto getVorlage(@PathVariable String kategorie, @PathVariable String vorlagenName,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return ApiMapper.dto(vorlagen.lies(kategorie, vorlagenName, stichtag));
    }

    @PutMapping("/{kategorie}/{vorlagenName}")
    VorlageDto updateVorlage(@PathVariable String kategorie, @PathVariable String vorlagenName,
                             @RequestBody VorlageUpdateRequestDto request) {
        return ApiMapper.dto(vorlagen.aendere(kategorie, vorlagenName, request.bezeichnung(), request.beschreibung(), request.tags()));
    }

    @DeleteMapping("/{kategorie}/{vorlagenName}")
    ResponseEntity<Void> deleteVorlage(@PathVariable String kategorie, @PathVariable String vorlagenName) {
        vorlagen.loesche(kategorie, vorlagenName);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{kategorie}/{vorlagenName}/veroeffentlichung")
    VorlageDto setVorlageVeroeffentlichung(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                           @RequestBody ZeitraumDto zeitraum) {
        return ApiMapper.dto(vorlagen.setzeZeitraum(kategorie, vorlagenName, new Zeitraum(zeitraum.ab(), zeitraum.bis())));
    }

    @DeleteMapping("/{kategorie}/{vorlagenName}/veroeffentlichung")
    ResponseEntity<Void> deleteVorlageVeroeffentlichung(@PathVariable String kategorie, @PathVariable String vorlagenName) {
        vorlagen.setzeZeitraum(kategorie, vorlagenName, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{kategorie}/{vorlagenName}/inhalt")
    ResponseEntity<byte[]> getAufgeloestenInhalt(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                                 @RequestParam(required = false) String version,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return Http.binaer(vorlagen.aufgeloesterInhalt(kategorie, vorlagenName,
                VersionsAngabe.parseOptional(version).orElse(null), stichtag));
    }

    @GetMapping("/{kategorie}/{vorlagenName}/verwendungen")
    List<VerwendungDto> getVerwendungen(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag,
                                        @RequestParam(defaultValue = "false") boolean nurNutzbar) {
        return vorlagen.verwendungen(kategorie, vorlagenName, stichtag, nurNutzbar).stream().map(ApiMapper::dto).toList();
    }
}
