package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.AufgeloesteBausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.BausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionResponseDto;
import de.dpma.qd.service.dokumentenerstellung.impl.HochgeladeneDatei;
import de.dpma.qd.service.dokumentenerstellung.impl.InhaltDaten;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionService;

/** Inhalt, Schema und Baustein-Referenzen einer Version; implementiert {@link VersionsinhalteApi}. */
@RestController
public class VersionsinhalteResource implements VersionsinhalteApi {

    private final VersionService versionen;

    public VersionsinhalteResource(VersionService versionen) {
        this.versionen = versionen;
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<Resource> getInhalt(String kategorie, String vorlagenName, String version) {
        InhaltDaten inhalt = versionen.inhalt(kategorie, vorlagenName, InboundApiMapper.version(version));
        return BinaerAntwort.von(inhalt.daten(), inhalt.ref().format(), inhalt.dateiname(), inhalt.version());
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> updateInhalt(String kategorie, String vorlagenName, String version, MultipartFile inhalt) {
        return ResponseEntity.ok(OutboundApiMapper.dto(versionen.ersetzeInhalt(kategorie, vorlagenName, InboundApiMapper.version(version),
                InboundApiMapper.datei(inhalt))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<String> getSchema(String kategorie, String vorlagenName, String version) {
        return ResponseEntity.ok(new String(versionen.schema(kategorie, vorlagenName, InboundApiMapper.version(version)), StandardCharsets.UTF_8));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> updateSchema(String kategorie, String vorlagenName, String version, String schema) {
        return ResponseEntity.ok(OutboundApiMapper.dto(versionen.ersetzeSchema(kategorie, vorlagenName, InboundApiMapper.version(version),
                new HochgeladeneDatei("schema.xsd", schema.getBytes(StandardCharsets.UTF_8)))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<List<AufgeloesteBausteinReferenzDto>> getBausteinReferenzen(String kategorie, String vorlagenName, String version,
            OffsetDateTime stichtag) {
        return ResponseEntity.ok(OutboundApiMapper.referenzen(versionen.bausteine(kategorie, vorlagenName, InboundApiMapper.version(version),
                InboundApiMapper.zeitpunkt(stichtag))));
    }

    @Override
    @Secured(Rollen.FORMULAR_VERWALTUNG)
    public ResponseEntity<VersionResponseDto> updateBausteinReferenzen(String kategorie, String vorlagenName, String version,
            List<BausteinReferenzDto> referenzen) {
        return ResponseEntity.ok(OutboundApiMapper.dto(versionen.setzeBausteine(kategorie, vorlagenName, InboundApiMapper.version(version),
                InboundApiMapper.referenzen(referenzen))));
    }
}
