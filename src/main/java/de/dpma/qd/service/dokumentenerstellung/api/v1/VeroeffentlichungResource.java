package de.dpma.qd.service.dokumentenerstellung.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlageResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.ZeitraumDto;
import de.dpma.qd.service.dokumentenerstellung.impl.VeroeffentlichungService;

/** Veröffentlichung planen, verschieben, absagen, zurückziehen; implementiert {@link VeroeffentlichungApi}. */
@RestController
public class VeroeffentlichungResource implements VeroeffentlichungApi {

    private final VeroeffentlichungService veroeffentlichung;

    public VeroeffentlichungResource(VeroeffentlichungService veroeffentlichung) {
        this.veroeffentlichung = veroeffentlichung;
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> setVersionVeroeffentlichung(String kategorie, String vorlagenName, String version,
            ZeitraumDto zeitraum) {
        return ResponseEntity.ok(OutboundApiMapper.dto(veroeffentlichung.setzeVersion(kategorie, vorlagenName, InboundApiMapper.version(version),
                InboundApiMapper.zeitpunkt(zeitraum.ab()), InboundApiMapper.zeitpunkt(zeitraum.bis()))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> deleteVersionVeroeffentlichung(String kategorie, String vorlagenName, String version) {
        return ResponseEntity.ok(OutboundApiMapper.dto(veroeffentlichung.sageVersionAb(kategorie, vorlagenName, InboundApiMapper.version(version))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VorlageResponseDto> setVorlageVeroeffentlichung(String kategorie, String vorlagenName, ZeitraumDto zeitraum) {
        return ResponseEntity.ok(OutboundApiMapper.dto(veroeffentlichung.setzeVorlage(kategorie, vorlagenName, InboundApiMapper.zeitraum(zeitraum))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<Void> deleteVorlageVeroeffentlichung(String kategorie, String vorlagenName) {
        veroeffentlichung.setzeVorlage(kategorie, vorlagenName, null);
        return ResponseEntity.noContent().build();
    }
}
