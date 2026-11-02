package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinName;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenFilter;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.VorlagenRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter: {@link VorlagenRepository} auf Oracle via JPA. Bildet das Aggregat {@link Vorlage} auf
 * DE_VORLAGE, DE_VORLAGE_TAG, DE_VERSION und DE_BAUSTEIN_REF ab; Inhalte werden nur referenziert.
 */
@Repository
class JpaVorlagenRepository implements VorlagenRepository {

    private final VorlageJpaRepository vorlagen;
    private final KategorieJpaRepository kategorien;
    private final InhaltJpaRepository inhalte;

    JpaVorlagenRepository(VorlageJpaRepository vorlagen, KategorieJpaRepository kategorien, InhaltJpaRepository inhalte) {
        this.vorlagen = vorlagen;
        this.kategorien = kategorien;
        this.inhalte = inhalte;
    }

    @Override
    public Optional<Vorlage> finde(String kategorie, String name) {
        return vorlagen.finde(kategorie, name).map(PersistenzMapper::zuDomain);
    }

    @Override
    public Optional<Vorlage> findeZumAendern(String kategorie, String name) {
        return vorlagen.finde(kategorie, name)
                .flatMap(e -> vorlagen.findeMitSperre(e.getId()))
                .map(PersistenzMapper::zuDomain);
    }

    @Override
    public List<Vorlage> suche(VorlagenFilter filter) {
        return vorlagen.findAll(VorlagenSpezifikation.aus(filter)).stream().map(PersistenzMapper::zuDomain).toList();
    }

    @Override
    public List<Vorlage> findeReferenzierende(BausteinName baustein) {
        return vorlagen.finde(baustein.kategorie(), baustein.name())
                .map(b -> vorlagen.findeReferenzierende(b.getId()).stream().map(PersistenzMapper::zuDomain).toList())
                .orElse(List.of());
    }

    @Override
    public Vorlage speichere(Vorlage vorlage) {
        VorlageEntity e;
        if (vorlage.id() == null) {
            e = new VorlageEntity();
            e.setKategorie(kategorien.findByName(vorlage.kategorie()).orElseThrow(() -> new NichtGefundenException(
                    "KATEGORIE_NICHT_GEFUNDEN", "Kategorie " + vorlage.kategorie() + " nicht gefunden")));
            e.setName(vorlage.name());
            e.setTyp(vorlage.typ());
        } else {
            e = vorlagen.findById(vorlage.id()).orElseThrow(() -> new NichtGefundenException("VORLAGE_NICHT_GEFUNDEN",
                    "Vorlage " + vorlage.vollerName() + " wurde zwischenzeitlich gelöscht"));
            if (vorlage.revision() != null && !vorlage.revision().equals(e.getRevision())) {
                throw new KonfliktException("GLEICHZEITIGE_AENDERUNG",
                        "Vorlage " + vorlage.vollerName() + " wurde zwischenzeitlich geändert");
            }
        }
        e.setBezeichnung(vorlage.bezeichnung());
        e.setBeschreibung(vorlage.beschreibung());
        if (!e.getTags().equals(vorlage.tags())) {      // nur bei Änderung, sonst schreibt Hibernate die Collection neu
            e.getTags().retainAll(vorlage.tags());
            e.getTags().addAll(vorlage.tags());
        }
        e.setVeroeffentlichtAb(vorlage.veroeffentlichung() == null ? null : vorlage.veroeffentlichung().ab());
        e.setVeroeffentlichtBis(vorlage.veroeffentlichung() == null ? null : vorlage.veroeffentlichung().bis());
        e.setAudit(PersistenzMapper.zuEntity(vorlage.audit()));
        uebertrageVersionen(vorlage, e);
        return PersistenzMapper.zuDomain(vorlagen.saveAndFlush(e));
    }

    @Override
    public void loesche(Vorlage vorlage) {
        vorlagen.deleteById(vorlage.id());
    }

    // -------------------------------------------------------------------------------------

    private void uebertrageVersionen(Vorlage vorlage, VorlageEntity e) {
        // Bausteine zuerst auflösen: Abfragen mitten im Umbau würden einen vorzeitigen Auto-Flush auslösen
        Map<BausteinName, VorlageEntity> bausteine = new HashMap<>();
        vorlage.versionen().stream().flatMap(v -> v.bausteine().stream()).map(BausteinReferenz::baustein).distinct()
                .forEach(n -> bausteine.put(n, vorlagen.finde(n.kategorie(), n.name())
                        .orElseThrow(() -> new NichtGefundenException("BAUSTEIN_UNBEKANNT", "Baustein " + n + " nicht gefunden"))));

        Map<Long, VersionEntity> vorhanden = e.getVersionen().stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(VersionEntity::getId, Function.identity()));
        Set<Long> imAggregat = vorlage.versionen().stream().map(Version::id).filter(Objects::nonNull).collect(Collectors.toSet());
        e.getVersionen().removeIf(v -> v.getId() != null && !imAggregat.contains(v.getId()));

        for (Version version : vorlage.versionen()) {
            VersionEntity ve;
            if (version.id() == null) {
                ve = new VersionEntity();
                ve.setVorlage(e);
                ve.setHaupt(version.nummer().haupt());
                ve.setNeben(version.nummer().neben());
                e.getVersionen().add(ve);
            } else {
                ve = Objects.requireNonNull(vorhanden.get(version.id()), "Version " + version.id() + " gehört nicht zur Vorlage");
            }
            ve.setStatus(version.status());
            ve.setVeroeffentlichtAb(version.zeitraum() == null ? null : version.zeitraum().ab());
            ve.setVeroeffentlichtBis(version.zeitraum() == null ? null : version.zeitraum().bis());
            ve.setKommentar(version.kommentar());
            ve.setInhalt(inhalt(version.inhalt()));
            ve.setSchema(version.schema() == null ? null : inhalt(version.schema()));
            ve.setAudit(PersistenzMapper.zuEntity(version.audit()));
            List<BausteinReferenz> bisher = ve.getBausteinReferenzen().stream().map(PersistenzMapper::zuDomain).toList();
            if (bisher.equals(version.bausteine())) {
                continue;                                    // unverändert: Collection nicht anfassen
            }
            ve.getBausteinReferenzen().clear();
            for (BausteinReferenz ref : version.bausteine()) {
                VorlageEntity baustein = bausteine.get(ref.baustein());
                ve.getBausteinReferenzen().add(new BausteinReferenzEmbeddable(baustein, ref.angabe().haupt(), ref.angabe().neben()));
            }
        }
    }

    private InhaltEntity inhalt(InhaltRef ref) {
        return inhalte.getReferenceById(ref.id());
    }
}
