package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionArtDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionResponseDto;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionService;

/** Versionen anlegen, abrufen und löschen; implementiert {@link VersionenApi}. */
@RestController
public class VersionenResource implements VersionenApi {

    private final VersionService versionen;

    public VersionenResource(VersionService versionen) {
        this.versionen = versionen;
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<List<VersionResponseDto>> listVersionen(String kategorie, String vorlagenName, OffsetDateTime stichtag) {
        return ResponseEntity.ok(versionen.liste(kategorie, vorlagenName, InboundApiMapper.zeitpunkt(stichtag)).stream().map(OutboundApiMapper::dto)
                .toList());
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> createVersion(String kategorie, String vorlagenName, VersionArtDto art, Integer basisHauptversion,
            String basisVersion, String kommentar, MultipartFile inhalt, MultipartFile schema) {
        VersionResponseDto dto = OutboundApiMapper.dto(versionen.legeAn(kategorie, vorlagenName, new VersionService.Antrag(InboundApiMapper.art(art),
                basisHauptversion, InboundApiMapper.version(basisVersion), kommentar, InboundApiMapper.datei(inhalt), InboundApiMapper.datei(schema))));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{version}").buildAndExpand(dto.version()).toUri())
                .body(dto);
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> getVersion(String kategorie, String vorlagenName, String version, OffsetDateTime stichtag) {
        return ResponseEntity.ok(OutboundApiMapper.dto(versionen.lies(kategorie, vorlagenName, InboundApiMapper.version(version),
                InboundApiMapper.zeitpunkt(stichtag))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<Void> deleteVersion(String kategorie, String vorlagenName, String version) {
        versionen.loesche(kategorie, vorlagenName, InboundApiMapper.version(version));
        return ResponseEntity.noContent().build();
    }
}
