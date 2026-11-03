package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.BausteinReferenzDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VersionArtDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.VorlagenTypDto;
import de.dpma.qd.service.dokumentenerstellung.api.v1.model.ZeitraumDto;
import de.dpma.qd.service.dokumentenerstellung.impl.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.impl.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.impl.FachlicherFehler;
import de.dpma.qd.service.dokumentenerstellung.impl.HochgeladeneDatei;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionArt;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.impl.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.impl.Zeitraum;

/** Mapping API-Eingaben (generierte DTOs, Parameter) → Fachobjekte. */
final class InboundApiMapper {

    private InboundApiMapper() {
    }

    static Instant zeitpunkt(OffsetDateTime zeitpunkt) {
        return zeitpunkt == null ? null : zeitpunkt.toInstant();
    }

    static Zeitraum zeitraum(ZeitraumDto dto) {
        return dto == null ? null : new Zeitraum(zeitpunkt(dto.ab()), zeitpunkt(dto.bis()));
    }

    static VersionsNummer version(String version) {
        return version == null ? null : VersionsNummer.parse(version);
    }

    static VersionsAngabe angabe(String version) {
        return VersionsAngabe.parseOptional(version).orElse(null);
    }

    static VorlagenTyp typ(VorlagenTypDto dto) {
        return dto == null ? null : VorlagenTyp.valueOf(dto.name());
    }

    static Set<VorlagenTyp> typen(List<VorlagenTypDto> dtos) {
        return dtos == null ? Set.of() : dtos.stream().map(InboundApiMapper::typ).collect(Collectors.toSet());
    }

    static VersionArt art(VersionArtDto dto) {
        return dto == null ? null : VersionArt.valueOf(dto.name());
    }

    static Set<String> menge(List<String> werte) {
        return werte == null ? Set.of() : Set.copyOf(werte);
    }

    static List<BausteinReferenz> referenzen(List<BausteinReferenzDto> dtos) {
        return dtos.stream().map(r -> new BausteinReferenz(BausteinName.parse(r.baustein()), VersionsAngabe.parse(r.version()))).toList();
    }

    static HochgeladeneDatei datei(MultipartFile datei) {
        if (datei == null) {
            return null;
        }
        if (datei.isEmpty()) {
            throw FachlicherFehler.ungueltig("INHALT_LEER", "Datei '" + datei.getOriginalFilename() + "' ist leer");
        }
        try {
            return new HochgeladeneDatei(datei.getOriginalFilename(), datei.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
