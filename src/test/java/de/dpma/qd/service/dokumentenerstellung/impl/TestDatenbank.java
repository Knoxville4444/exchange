package de.dpma.qd.service.dokumentenerstellung.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltDatenEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltDatenRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

/**
 * Hält Entities für Service-Tests und beantwortet darüber die Aufrufe der gemockten Repositories. Damit bleiben die Tests bei
 * „Mocks am Repository“, ohne jede Abfrage einzeln zu stubben. Die Stubs sind {@code lenient}, weil nicht jeder Test alle braucht.
 */
public final class TestDatenbank {

    private final KategorieEntity kategorie = new KategorieEntity();
    private final Map<String, VorlageEntity> vorlagen = new HashMap<>();
    private final Map<Long, InhaltEntity> inhalte = new HashMap<>();
    private final Map<Long, byte[]> daten = new HashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    public TestDatenbank() {
        kategorie.setName("PATGBM");
    }

    public void verdrahte(VorlageRepository vorlageRepository, InhaltRepository inhaltRepository, InhaltDatenRepository datenRepository) {
        lenient().when(vorlageRepository.finde(any(), any())).thenAnswer(inv -> finde(inv.getArgument(0), inv.getArgument(1)));
        lenient().when(vorlageRepository.findeZumAendern(any(), any())).thenAnswer(inv -> finde(inv.getArgument(0), inv.getArgument(1)));
        lenient().when(vorlageRepository.findeReferenzierende(any(), any()))
                .thenAnswer(inv -> referenzierende(inv.getArgument(0), inv.getArgument(1)));
        lenient().when(vorlageRepository.saveAndFlush(any(VorlageEntity.class))).thenAnswer(inv -> speichere(inv.getArgument(0)));
        lenient().when(inhaltRepository.getReferenceById(any())).thenAnswer(inv -> inhalte.get((Long) inv.getArgument(0)));
        lenient().when(inhaltRepository.findBySha256AndFormat(any(), any())).thenAnswer(inv -> inhalte.values().stream()
                .filter(i -> i.getSha256().equals(inv.getArgument(0)) && i.getFormat() == inv.getArgument(1)).findFirst());
        lenient().when(inhaltRepository.saveAndFlush(any(InhaltEntity.class))).thenAnswer(inv -> {
            InhaltEntity e = inv.getArgument(0);
            setzeId(e, ids.incrementAndGet());
            inhalte.put(e.getId(), e);
            return e;
        });
        lenient().when(datenRepository.save(any(InhaltDatenEntity.class))).thenAnswer(inv -> {
            InhaltDatenEntity e = inv.getArgument(0);
            daten.put(inhaltId(e), e.getDaten());
            return e;
        });
        lenient().when(datenRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(daten.get((Long) inv.getArgument(0)))
                .map(d -> new InhaltDatenEntity(inv.getArgument(0), d)));
    }

    /** Legt eine Vorlage samt Inhalten an (Inhaltsdaten = Name der Version, falls nicht anders angegeben). */
    public VorlageEntity lege(Vorlage vorlage) {
        vorlage.versionen().forEach(v -> {
            registriere(v.inhalt(), (vorlage.name() + " " + v.nummer()).getBytes());
            if (v.schema() != null) {
                registriere(v.schema(), "<xs:schema/>".getBytes());
            }
        });
        VorlageEntity e = new VorlageEntity();
        e.setKategorie(kategorie);
        e.setName(vorlage.name());
        e.setTyp(vorlage.typ());
        VorlageEntityMapper.uebertrage(vorlage, e, n -> finde(n.kategorie(), n.name()).orElseThrow(), ref -> inhalte.get(ref.id()));
        vorlagen.put(vorlage.vollerName(), e);
        return e;
    }

    public void registriere(InhaltRef ref, byte[] inhalt) {
        InhaltEntity e = new InhaltEntity(ref.sha256(), ref.format(), ref.groesse(), Testdaten.T0);
        setzeId(e, ref.id());
        inhalte.put(ref.id(), e);
        daten.put(ref.id(), inhalt);
    }

    public Vorlage vorlage(String kategorieName, String name) {
        return VorlageEntityMapper.zuFachobjekt(vorlagen.get(kategorieName + "/" + name));
    }

    public int anzahlInhalte() {
        return inhalte.size();
    }

    private Optional<VorlageEntity> finde(String kategorieName, String name) {
        return Optional.ofNullable(vorlagen.get(kategorieName + "/" + name));
    }

    private List<VorlageEntity> referenzierende(String kategorieName, String name) {
        List<VorlageEntity> ergebnis = new ArrayList<>();
        for (VorlageEntity v : vorlagen.values()) {
            boolean referenziert = v.getVersionen().stream().flatMap(ver -> ver.getBausteinReferenzen().stream())
                    .anyMatch(r -> r.getBaustein().getName().equals(name) && r.getBaustein().getKategorie().getName().equals(kategorieName));
            if (referenziert) {
                ergebnis.add(v);
            }
        }
        return ergebnis;
    }

    private VorlageEntity speichere(VorlageEntity e) {
        vorlagen.put(e.getKategorie().getName() + "/" + e.getName(), e);
        return e;
    }

    private static void setzeId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Long inhaltId(InhaltDatenEntity e) {
        try {
            Field f = InhaltDatenEntity.class.getDeclaredField("inhaltId");
            f.setAccessible(true);
            return (Long) f.get(e);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
