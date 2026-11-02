package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Inhaltsformat;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.InhaltSpeicher;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

/**
 * Adapter: Inhalte als BLOB in Oracle (DE_INHALT + DE_INHALT_DATEN), dedupliziert über SHA-256.
 * <p>
 * Läuft in der Transaktion des Services – schlägt die fachliche Operation fehl, verschwindet auch der
 * gespeicherte Inhalt (kein Aufräumen verwaister Dateien nötig).
 * <p>
 * Ein Dateisystem- oder S3-Adapter implementiert später einfach {@link InhaltSpeicher}; DE_INHALT bleibt als
 * Metadatentabelle erhalten, nur DE_INHALT_DATEN entfällt.
 */
@Repository
class DatenbankInhaltSpeicher implements InhaltSpeicher {

    private final InhaltJpaRepository inhalte;
    private final InhaltDatenJpaRepository daten;
    private final Clock uhr;

    DatenbankInhaltSpeicher(InhaltJpaRepository inhalte, InhaltDatenJpaRepository daten, Clock uhr) {
        this.inhalte = inhalte;
        this.daten = daten;
        this.uhr = uhr;
    }

    @Override
    public InhaltRef speichere(byte[] inhalt, Inhaltsformat format) {
        String sha = sha256(inhalt);
        InhaltEntity e = inhalte.findBySha256AndFormat(sha, format).orElseGet(() -> {
            InhaltEntity neu = inhalte.saveAndFlush(new InhaltEntity(sha, format, inhalt.length, uhr.instant()));
            daten.save(new InhaltDatenEntity(neu.getId(), inhalt));
            return neu;
        });
        return PersistenzMapper.zuDomain(e);
    }

    @Override
    public byte[] lade(InhaltRef ref) {
        return daten.findById(ref.id()).map(InhaltDatenEntity::getDaten)
                .orElseThrow(() -> new IllegalStateException("Inhalt " + ref.id() + " fehlt in DE_INHALT_DATEN"));
    }

    private static String sha256(byte[] inhalt) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(inhalt));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
