package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.KategorieRequestDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.KategorieResponseDto;
import de.dpma.qd.service.dokumentenerstellung.impl.KategorieService;

/** Kategorien (≙ Fachsysteme); implementiert das aus der OpenAPI generierte {@link KategorienApi}. */
@RestController
public class KategorienResource implements KategorienApi {

    private final KategorieService kategorien;

    public KategorienResource(KategorieService kategorien) {
        this.kategorien = kategorien;
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<List<KategorieResponseDto>> listKategorien() {
        return ResponseEntity.ok(kategorien.alle().stream().map(OutboundApiMapper::dto).toList());
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<KategorieResponseDto> putKategorie(String kategorie, KategorieRequestDto request) {
        return ResponseEntity.ok(OutboundApiMapper.dto(kategorien.speichere(kategorie, request.beschreibung())));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<Void> deleteKategorie(String kategorie) {
        kategorien.loesche(kategorie);
        return ResponseEntity.noContent().build();
    }
}
