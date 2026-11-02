package de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Kategorie;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.KategorieRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class InMemoryKategorieRepository implements KategorieRepository {

    private final Map<String, Kategorie> kategorien = new TreeMap<>();
    private final Supplier<Map<String, Long>> anzahl;
    private long naechsteId = 1;

    public InMemoryKategorieRepository(InMemoryVorlagenRepository vorlagen) {
        this.anzahl = () -> vorlagen.alle().stream().collect(Collectors.groupingBy(v -> v.kategorie(), Collectors.counting()));
    }

    @Override
    public List<Kategorie> alle() {
        return List.copyOf(kategorien.values());
    }

    @Override
    public Optional<Kategorie> finde(String name) {
        return Optional.ofNullable(kategorien.get(name));
    }

    @Override
    public Kategorie speichere(Kategorie k) {
        Kategorie gespeichert = k.id() == null ? new Kategorie(naechsteId++, k.name(), k.beschreibung(), k.audit()) : k;
        kategorien.put(k.name(), gespeichert);
        return gespeichert;
    }

    @Override
    public void loesche(String name) {
        kategorien.remove(name);
    }

    @Override
    public Map<String, Long> anzahlVorlagen() {
        return anzahl.get();
    }
}
