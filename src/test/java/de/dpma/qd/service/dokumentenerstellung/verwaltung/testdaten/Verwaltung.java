package de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.KategorieService;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VersionService;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VorlagenService;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VorlagenZugriff;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung.VorlagenAufloesungService;

import java.time.Instant;

/** Verdrahtet die Services mit In-Memory-Adaptern – ohne Spring. */
public final class Verwaltung {

    public final VerstellbareUhr uhr;
    public final InMemoryVorlagenRepository vorlagenRepository = new InMemoryVorlagenRepository();
    public final InMemoryKategorieRepository kategorieRepository = new InMemoryKategorieRepository(vorlagenRepository);
    public final InMemoryInhaltSpeicher inhalte = new InMemoryInhaltSpeicher();
    public final KategorieService kategorien;
    public final VorlagenService vorlagen;
    public final VersionService versionen;
    public final VorlagenAufloesungService aufloesung;

    public Verwaltung(Instant start) {
        this.uhr = new VerstellbareUhr(start);
        VorlagenZugriff zugriff = new VorlagenZugriff(vorlagenRepository, inhalte, () -> "tester", uhr);
        this.kategorien = new KategorieService(kategorieRepository, zugriff);
        this.vorlagen = new VorlagenService(vorlagenRepository, kategorieRepository, zugriff);
        this.versionen = new VersionService(zugriff);
        this.aufloesung = new VorlagenAufloesungService(zugriff);
    }
}
