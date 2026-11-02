package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.KategorieDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.KategorieRequestDto;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.KategorieService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Skizze: Kategorien. In der Umsetzung implementiert der Controller das generierte API-Interface. */
@RestController
@RequestMapping(Http.BASIS + "/kategorien")
@PreAuthorize("hasAuthority('SCOPE_formular-verwaltung')")
class KategorienController {

    private final KategorieService kategorien;

    KategorienController(KategorieService kategorien) {
        this.kategorien = kategorien;
    }

    @GetMapping
    List<KategorieDto> listKategorien() {
        return kategorien.alle().stream().map(ApiMapper::dto).toList();
    }

    @PutMapping("/{kategorie}")
    KategorieDto putKategorie(@PathVariable String kategorie, @RequestBody KategorieRequestDto request) {
        return ApiMapper.dto(kategorien.speichere(kategorie, request.beschreibung()));
    }

    @DeleteMapping("/{kategorie}")
    ResponseEntity<Void> deleteKategorie(@PathVariable String kategorie) {
        kategorien.loesche(kategorie);
        return ResponseEntity.noContent().build();
    }
}
