package de.dpma.qd.service.dokumentenerstellung.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltDatenEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltDatenRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;

/**
 * Speichert und lädt Inhalte (ODT, PDF, XSD). Die Daten liegen als BLOB in einer eigenen Tabelle und werden über SHA-256
 * dedupliziert. Sie werden in der Transaktion des Aufrufers geschrieben: Schlägt die fachliche Operation fehl, verschwindet auch
 * der Inhalt.
 */
@Service
public class InhaltService {

    private final InhaltRepository inhalte;
    private final InhaltDatenRepository daten;
    private final BausteinService bausteine;
    private final AuditErzeuger audit;

    public InhaltService(InhaltRepository inhalte, InhaltDatenRepository daten, BausteinService bausteine, AuditErzeuger audit) {
        this.inhalte = inhalte;
        this.daten = daten;
        this.bausteine = bausteine;
        this.audit = audit;
    }

    /** Inhalt einer Version samt abgeglichener Baustein-Referenzen. */
    public record VersionsInhalt(InhaltRef inhalt, List<BausteinReferenz> bausteine) {
    }

    /**
     * Prüft und speichert den Inhalt einer Version. Bei Formularen werden die Baustein-Bereiche erkannt und mit den bisherigen
     * Referenzen abgeglichen; Bausteine dürfen selbst keine Bausteine enthalten.
     */
    public VersionsInhalt speichereVersionsinhalt(VorlagenTyp typ, HochgeladeneDatei datei, List<BausteinReferenz> bisher) {
        InhaltRef ref = speichere(datei, typ.inhaltsformat());
        if (typ.inhaltsformat() != Inhaltsformat.ODT) {
            return new VersionsInhalt(ref, List.of());
        }
        Set<BausteinName> bereiche = InhaltPruefung.bausteinBereiche(datei.daten());
        if (typ == VorlagenTyp.BAUSTEIN) {
            if (!bereiche.isEmpty()) {
                throw FachlicherFehler.ungueltig("VERSCHACHTELTE_BAUSTEINE", "Bausteine dürfen keine Bausteine enthalten");
            }
            return new VersionsInhalt(ref, List.of());
        }
        return new VersionsInhalt(ref, BausteinPruefung.gleicheAb(bereiche, bisher, bausteine::finde));
    }

    public InhaltRef speichereSchema(HochgeladeneDatei datei) {
        return speichere(datei, Inhaltsformat.XSD);
    }

    public byte[] lade(InhaltRef ref) {
        return daten.findById(ref.id()).map(InhaltDatenEntity::getDaten)
                .orElseThrow(() -> new IllegalStateException("Inhalt " + ref.id() + " fehlt in INHALT_DATEN"));
    }

    private InhaltRef speichere(HochgeladeneDatei datei, Inhaltsformat format) {
        InhaltPruefung.pruefe(datei.daten(), format);
        String sha = sha256(datei.daten());
        InhaltEntity entity = inhalte.findBySha256AndFormat(sha, format).orElseGet(() -> {
            InhaltEntity neu = inhalte.saveAndFlush(new InhaltEntity(sha, format, datei.daten().length, audit.jetzt()));
            daten.save(new InhaltDatenEntity(neu.getId(), datei.daten()));
            return neu;
        });
        return VorlageEntityMapper.zuFachobjekt(entity);
    }

    private static String sha256(byte[] inhalt) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(inhalt));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
