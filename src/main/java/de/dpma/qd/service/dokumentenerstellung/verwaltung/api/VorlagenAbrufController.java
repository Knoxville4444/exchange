package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.AufloesungDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung.AufgeloesteVorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung.VorlagenAufloesung;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Skizze: die beiden öffentlichen Endpunkte, die ohne LibreOffice auskommen – Versionsauflösung und
 * Merkblatt-Auslieferung. Die Erzeugung (ODT/PDF/A) liegt beim bestehenden Erzeugungsteil.
 */
@RestController
@RequestMapping(Http.BASIS + "/vorlagen/{kategorie}/{vorlagenName}")
@PreAuthorize("hasAuthority('SCOPE_formular-public')")
class VorlagenAbrufController {

    private final VorlagenAufloesung aufloesung;

    VorlagenAbrufController(VorlagenAufloesung aufloesung) {
        this.aufloesung = aufloesung;
    }

    @GetMapping("/aufloesung")
    AufloesungDto loeseVersionAuf(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                  @RequestParam(required = false) String version,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        return ApiMapper.dto(aufloesung.ermittle(kategorie, vorlagenName, VersionsAngabe.parseOptional(version).orElse(null), stichtag));
    }

    @GetMapping("/merkblatt")
    ResponseEntity<byte[]> getMerkblatt(@PathVariable String kategorie, @PathVariable String vorlagenName,
                                        @RequestParam(required = false) String version,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant stichtag) {
        AufgeloesteVorlage merkblatt = aufloesung.loese(kategorie, vorlagenName,
                VersionsAngabe.parseOptional(version).orElse(null), stichtag, VorlagenTyp.MERKBLATT);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(merkblatt.name() + "_" + merkblatt.version() + ".pdf").build().toString())
                .header(Http.X_FORMULAR_VERSION, merkblatt.version().toString())
                .body(merkblatt.inhalt());
    }
}
