package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.dpma.qd.service.dokumentenerstellung.impl.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.impl.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.impl.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.impl.Version;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionStatus;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.impl.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.impl.Zeitraum;

/**
 * Mapping Vorlage-Entity (inkl. Versionen, Tags, Baustein-Referenzen) ↔ Fachobjekt {@link Vorlage}.
 * <p>
 * Versionen werden über ihre Nummer abgeglichen, die innerhalb einer Vorlage eindeutig ist. Collections werden nur
 * angefasst, wenn sie sich geändert haben, sonst schreibt Hibernate sie bei jedem Speichern neu.
 */
public final class VorlageEntityMapper {

    private VorlageEntityMapper() {
    }

    public static Vorlage zuFachobjekt(VorlageEntity e) {
        return new Vorlage(e.getKategorie().getName(), e.getName(), e.getTyp(), e.getBezeichnung(), e.getBeschreibung(),
                new HashSet<>(e.getTags()), zeitraum(e.getVeroeffentlichtAb(), e.getVeroeffentlichtBis()),
                KategorieEntityMapper.zuFachobjekt(e.getAudit()), e.getVersionen().stream().map(VorlageEntityMapper::zuFachobjekt).toList());
    }

    static Version zuFachobjekt(VorlageVersionEntity e) {
        Zeitraum zeitraum = e.getStatus() == VersionStatus.DRAFT ? null : new Zeitraum(e.getVeroeffentlichtAb(), e.getVeroeffentlichtBis());
        return new Version(new VersionsNummer(e.getHaupt(), e.getNeben()), e.getStatus(), zeitraum, e.getKommentar(),
                zuFachobjekt(e.getInhalt()), e.getSchema() == null ? null : zuFachobjekt(e.getSchema()),
                e.getBausteinReferenzen().stream().map(VorlageEntityMapper::zuFachobjekt).toList(),
                KategorieEntityMapper.zuFachobjekt(e.getAudit()));
    }

    static BausteinReferenz zuFachobjekt(BausteinReferenzEmbeddable e) {
        VorlageEntity baustein = e.getBaustein();
        return new BausteinReferenz(new BausteinName(baustein.getKategorie().getName(), baustein.getName()),
                new VersionsAngabe(e.getAngabeHaupt(), e.getAngabeNeben()));
    }

    public static InhaltRef zuFachobjekt(InhaltEntity e) {
        return new InhaltRef(e.getId(), e.getSha256(), e.getFormat(), e.getGroesse());
    }

    /**
     * Überträgt das Fachobjekt auf die Entity. Kategorie, Name und Typ setzt der Aufrufer bei neuen Vorlagen.
     *
     * @param bausteine liefert die Entity eines referenzierten Bausteins
     * @param inhalte   liefert die Entity (bzw. Referenz) eines Inhalts
     */
    public static void uebertrage(Vorlage vorlage, VorlageEntity e, Function<BausteinName, VorlageEntity> bausteine,
            Function<InhaltRef, InhaltEntity> inhalte) {
        e.setBezeichnung(vorlage.bezeichnung());
        e.setBeschreibung(vorlage.beschreibung());
        if (!e.getTags().equals(vorlage.tags())) {
            e.getTags().retainAll(vorlage.tags());
            e.getTags().addAll(vorlage.tags());
        }
        e.setVeroeffentlichtAb(vorlage.veroeffentlichung() == null ? null : vorlage.veroeffentlichung().ab());
        e.setVeroeffentlichtBis(vorlage.veroeffentlichung() == null ? null : vorlage.veroeffentlichung().bis());
        e.setAudit(KategorieEntityMapper.zuEntity(vorlage.audit()));

        Set<VersionsNummer> imFachobjekt = vorlage.versionen().stream().map(Version::nummer).collect(Collectors.toSet());
        e.getVersionen().removeIf(v -> !imFachobjekt.contains(nummer(v)));
        Map<VersionsNummer, VorlageVersionEntity> vorhanden = e.getVersionen().stream()
                .collect(Collectors.toMap(VorlageEntityMapper::nummer, Function.identity()));
        for (Version version : vorlage.versionen()) {
            VorlageVersionEntity ve = vorhanden.get(version.nummer());
            if (ve == null) {
                ve = new VorlageVersionEntity();
                ve.setVorlage(e);
                ve.setHaupt(version.nummer().haupt());
                ve.setNeben(version.nummer().neben());
                e.getVersionen().add(ve);
            }
            uebertrage(version, ve, bausteine, inhalte);
        }
    }

    /** Wie {@link #uebertrage(Vorlage, VorlageEntity, Function, Function)}; Bausteine und Inhalte kommen aus den Repositories. */
    public static void uebertrage(Vorlage vorlage, VorlageEntity e, VorlageRepository vorlagen, InhaltRepository inhalte) {
        uebertrage(vorlage, e,
                n -> vorlagen.finde(n.kategorie(), n.name()).orElseThrow(() -> new IllegalStateException("Baustein " + n + " fehlt")),
                ref -> inhalte.getReferenceById(ref.id()));
    }

    private static void uebertrage(Version version, VorlageVersionEntity ve, Function<BausteinName, VorlageEntity> bausteine,
            Function<InhaltRef, InhaltEntity> inhalte) {
        ve.setStatus(version.status());
        ve.setVeroeffentlichtAb(version.zeitraum() == null ? null : version.zeitraum().ab());
        ve.setVeroeffentlichtBis(version.zeitraum() == null ? null : version.zeitraum().bis());
        ve.setKommentar(version.kommentar());
        ve.setInhalt(inhalte.apply(version.inhalt()));
        ve.setSchema(version.schema() == null ? null : inhalte.apply(version.schema()));
        ve.setAudit(KategorieEntityMapper.zuEntity(version.audit()));
        List<BausteinReferenz> bisher = ve.getBausteinReferenzen().stream().map(VorlageEntityMapper::zuFachobjekt).toList();
        if (!bisher.equals(version.bausteine())) {
            ve.getBausteinReferenzen().clear();
            for (BausteinReferenz ref : version.bausteine()) {
                ve.getBausteinReferenzen().add(new BausteinReferenzEmbeddable(bausteine.apply(ref.baustein()),
                        ref.angabe().haupt(), ref.angabe().neben()));
            }
        }
    }

    private static VersionsNummer nummer(VorlageVersionEntity e) {
        return new VersionsNummer(e.getHaupt(), e.getNeben());
    }

    private static Zeitraum zeitraum(Instant ab, Instant bis) {
        return ab == null && bis == null ? null : new Zeitraum(ab, bis);
    }
}
