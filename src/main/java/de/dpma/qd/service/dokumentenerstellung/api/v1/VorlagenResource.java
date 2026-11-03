package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VerwendungResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlageResponseDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlageUpdateRequestDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlagenTypDto;
import de.dpma.qd.service.dokumentenerstellung.impl.InhaltDaten;
import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenService;

/** Vorlagen: Suche, Anlage, Stammdaten, aufgelöster Inhalt, Verwendungsnachweis; implementiert {@link VorlagenApi}. */
@RestController
public class VorlagenResource implements VorlagenApi {

    private final VorlagenService vorlagen;

    public VorlagenResource(VorlagenService vorlagen) {
        this.vorlagen = vorlagen;
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<List<VorlageResponseDto>> sucheVorlagen(List<String> kategorie, List<VorlagenTypDto> typ, List<String> tag,
            String suchtext, OffsetDateTime stichtag) {
        return ResponseEntity.ok(vorlagen.suche(InboundApiMapper.menge(kategorie), InboundApiMapper.typen(typ), InboundApiMapper.menge(tag),
                suchtext, InboundApiMapper.zeitpunkt(stichtag)).stream().map(OutboundApiMapper::dto).toList());
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VorlageResponseDto> createVorlage(String kategorie, String vorlagenName, VorlagenTypDto typ, String bezeichnung,
            MultipartFile inhalt, String beschreibung, List<String> tags, String kommentar, MultipartFile schema) {
        VorlageResponseDto dto = OutboundApiMapper.dto(vorlagen.legeAn(kategorie, new VorlagenService.Antrag(vorlagenName,
                InboundApiMapper.typ(typ), bezeichnung, beschreibung, InboundApiMapper.menge(tags), kommentar, InboundApiMapper.datei(inhalt),
                InboundApiMapper.datei(schema))));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{name}").buildAndExpand(vorlagenName).toUri())
                .body(dto);
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VorlageResponseDto> getVorlage(String kategorie, String vorlagenName, OffsetDateTime stichtag) {
        return ResponseEntity.ok(OutboundApiMapper.dto(vorlagen.lies(kategorie, vorlagenName, InboundApiMapper.zeitpunkt(stichtag))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VorlageResponseDto> updateVorlage(String kategorie, String vorlagenName, VorlageUpdateRequestDto request) {
        return ResponseEntity.ok(OutboundApiMapper.dto(vorlagen.aendere(kategorie, vorlagenName, request.bezeichnung(), request.beschreibung(),
                request.tags())));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<Void> deleteVorlage(String kategorie, String vorlagenName) {
        vorlagen.loesche(kategorie, vorlagenName);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<Resource> getAufgeloestenInhalt(String kategorie, String vorlagenName, String version, OffsetDateTime stichtag) {
        InhaltDaten inhalt = vorlagen.aufgeloesterInhalt(kategorie, vorlagenName, InboundApiMapper.angabe(version), InboundApiMapper.zeitpunkt(stichtag));
        return BinaerAntwort.von(inhalt.daten(), inhalt.ref().format(), inhalt.dateiname(), inhalt.version());
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<List<VerwendungResponseDto>> getVerwendungen(String kategorie, String vorlagenName, OffsetDateTime stichtag,
            Boolean nurNutzbar) {
        return ResponseEntity.ok(vorlagen.verwendungen(kategorie, vorlagenName, InboundApiMapper.zeitpunkt(stichtag), Boolean.TRUE.equals(nurNutzbar))
                .stream().map(OutboundApiMapper::dto).toList());
    }
}
