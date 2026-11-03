package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.time.OffsetDateTime;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RestController;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.AufloesungResponseDto;
import de.dpma.qd.service.dokumentenerstellung.impl.AufgeloesteVorlage;
import de.dpma.qd.service.dokumentenerstellung.impl.Inhaltsformat;
import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenAufloesungService;
import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenTyp;

/**
 * Merkblatt-Auslieferung und Versionsauflösung; implementiert {@link AbrufApi}. Die Erzeugung ({@code ErzeugungApi}) implementiert der
 * Erzeugungsteil des Dienstes.
 */
@RestController
public class AbrufResource implements AbrufApi {

    private final VorlagenAufloesungService aufloesung;

    public AbrufResource(VorlagenAufloesungService aufloesung) {
        this.aufloesung = aufloesung;
    }

    @Override
    @Secured(Rollen.FORMULAR_NUTZER)
    public ResponseEntity<Resource> getMerkblatt(String kategorie, String vorlagenName, String version, OffsetDateTime stichtag) {
        AufgeloesteVorlage merkblatt = aufloesung.loese(kategorie, vorlagenName, InboundApiMapper.angabe(version), InboundApiMapper.zeitpunkt(stichtag),
                VorlagenTyp.MERKBLATT);
        return BinaerAntwort.von(merkblatt.inhalt(), Inhaltsformat.PDF, merkblatt.name() + "_" + merkblatt.version() + ".pdf", merkblatt.version());
    }

    @Override
    @Secured(Rollen.FORMULAR_NUTZER)
    public ResponseEntity<AufloesungResponseDto> loeseVersionAuf(String kategorie, String vorlagenName, String version, OffsetDateTime stichtag) {
        return ResponseEntity.ok(OutboundApiMapper.dto(aufloesung.ermittle(kategorie, vorlagenName, InboundApiMapper.angabe(version),
                InboundApiMapper.zeitpunkt(stichtag))));
    }
}
