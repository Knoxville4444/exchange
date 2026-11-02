package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Audit;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Kategorie;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionStatus;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Zeitraum;

import java.util.HashSet;

/** Abbildung zwischen Domänenobjekten und JPA-Entities. */
final class PersistenzMapper {

    private PersistenzMapper() {
    }

    static Kategorie zuDomain(KategorieEntity e) {
        return new Kategorie(e.getId(), e.getName(), e.getBeschreibung(), zuDomain(e.getAudit()));
    }

    static Vorlage zuDomain(VorlageEntity e) {
        return Vorlage.rekonstruiere(e.getId(), e.getKategorie().getName(), e.getName(), e.getTyp(), e.getBezeichnung(),
                e.getBeschreibung(), new HashSet<>(e.getTags()),
                zeitraum(e.getVeroeffentlichtAb(), e.getVeroeffentlichtBis()), zuDomain(e.getAudit()), e.getRevision(),
                e.getVersionen().stream().map(PersistenzMapper::zuDomain).toList());
    }

    static Version zuDomain(VersionEntity e) {
        Zeitraum zeitraum = e.getStatus() == VersionStatus.DRAFT ? null : new Zeitraum(e.getVeroeffentlichtAb(), e.getVeroeffentlichtBis());
        return Version.rekonstruiere(e.getId(), new VersionsNummer(e.getHaupt(), e.getNeben()), e.getStatus(), zeitraum,
                e.getKommentar(), zuDomain(e.getInhalt()), e.getSchema() == null ? null : zuDomain(e.getSchema()),
                e.getBausteinReferenzen().stream().map(PersistenzMapper::zuDomain).toList(), zuDomain(e.getAudit()));
    }

    static BausteinReferenz zuDomain(BausteinReferenzEmbeddable e) {
        VorlageEntity b = e.getBaustein();
        return new BausteinReferenz(new BausteinName(b.getKategorie().getName(), b.getName()),
                new VersionsAngabe(e.getAngabeHaupt(), e.getAngabeNeben()));
    }

    static InhaltRef zuDomain(InhaltEntity e) {
        return new InhaltRef(e.getId(), e.getSha256(), e.getFormat(), e.getGroesse());
    }

    static Audit zuDomain(AuditEmbeddable e) {
        return new Audit(e.getErstelltAm(), e.getErstelltVon(), e.getGeaendertAm(), e.getGeaendertVon());
    }

    static AuditEmbeddable zuEntity(Audit a) {
        return new AuditEmbeddable(a.erstelltAm(), a.erstelltVon(), a.geaendertAm(), a.geaendertVon());
    }

    private static Zeitraum zeitraum(java.time.Instant ab, java.time.Instant bis) {
        return ab == null && bis == null ? null : new Zeitraum(ab, bis);
    }
}
