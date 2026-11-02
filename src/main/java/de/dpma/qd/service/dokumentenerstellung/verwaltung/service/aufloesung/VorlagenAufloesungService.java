package de.dpma.qd.service.dokumentenerstellung.verwaltung.service.aufloesung;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.BausteinReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Version;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsAngabe;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionsNummer;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Vorlage;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.AufgeloesteReferenz;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.VorlagenZugriff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Implementierung der Nahtstelle zur Erzeugung. */
@Service
@Transactional(readOnly = true)
public class VorlagenAufloesungService implements VorlagenAufloesung {

    private final VorlagenZugriff zugriff;

    public VorlagenAufloesungService(VorlagenZugriff zugriff) {
        this.zugriff = zugriff;
    }

    @Override
    public AufgeloesteVorlage loese(String kategorie, String name, VersionsAngabe angabe, Instant stichtag,
                                    VorlagenTyp erwarteterTyp) {
        Instant t = zugriff.stichtagOderJetzt(stichtag);
        Vorlage vorlage = zugriff.lade(kategorie, name);
        if (erwarteterTyp != null) {
            zugriff.pruefeTyp(vorlage, erwarteterTyp);
        }
        Version version = vorlage.loese(angabe, t).orElseThrow(() -> keineVersion(vorlage, angabe, t));
        List<AufgeloesterBaustein> bausteine = version.bausteine().stream()
                .map(ref -> ladeBaustein(ref, baustein(ref).loese(ref.angabe(), t), t))
                .toList();
        return ergebnis(vorlage, version, t, bausteine);
    }

    @Override
    public AufgeloesteVorlage loeseFuerVorschau(String kategorie, String name, VersionsNummer nummer,
                                                boolean bausteinEntwuerfe, Instant stichtag) {
        Instant t = zugriff.stichtagOderJetzt(stichtag);
        Vorlage vorlage = zugriff.lade(kategorie, name);
        zugriff.pruefeTyp(vorlage, VorlagenTyp.FORMULAR);
        Version version = vorlage.version(nummer);
        List<AufgeloesterBaustein> bausteine = version.bausteine().stream()
                .map(ref -> {
                    Vorlage b = baustein(ref);
                    return ladeBaustein(ref, bausteinEntwuerfe ? b.hoechstePassendeVersion(ref.angabe()) : b.loese(ref.angabe(), t), t);
                })
                .toList();
        return ergebnis(vorlage, version, t, bausteine);
    }

    @Override
    public AufloesungsErgebnis ermittle(String kategorie, String name, VersionsAngabe angabe, Instant stichtag) {
        Instant t = zugriff.stichtagOderJetzt(stichtag);
        Vorlage vorlage = zugriff.lade(kategorie, name);
        Version version = vorlage.loese(angabe, t).orElseThrow(() -> keineVersion(vorlage, angabe, t));
        List<AufgeloesteReferenz> referenzen = zugriff.ansicht(vorlage, version, t).bausteine();
        return new AufloesungsErgebnis(t, vorlage.kategorie(), vorlage.name(), vorlage.typ(), version.nummer(), referenzen);
    }

    // -------------------------------------------------------------------------------------

    private Vorlage baustein(BausteinReferenz ref) {
        return zugriff.baustein(ref.baustein()).orElseThrow(() -> new NichtGefundenException("BAUSTEIN_NICHT_AUFLOESBAR",
                "Baustein " + ref.baustein() + " existiert nicht"));
    }

    private AufgeloesterBaustein ladeBaustein(BausteinReferenz ref, Optional<Version> version, Instant t) {
        Version v = version.orElseThrow(() -> new NichtGefundenException("BAUSTEIN_NICHT_AUFLOESBAR",
                "Baustein " + ref.baustein() + "@" + ref.angabe() + " ist zum " + t + " nicht auflösbar"));
        return new AufgeloesterBaustein(ref.baustein(), ref.baustein().bereichsname(), v.nummer(), zugriff.ladeInhalt(v.inhalt()));
    }

    private AufgeloesteVorlage ergebnis(Vorlage vorlage, Version version, Instant t, List<AufgeloesterBaustein> bausteine) {
        byte[] schema = version.schema() == null ? null : zugriff.ladeInhalt(version.schema());
        return new AufgeloesteVorlage(vorlage.kategorie(), vorlage.name(), vorlage.typ(), version.nummer(), t,
                zugriff.ladeInhalt(version.inhalt()), schema, bausteine);
    }

    private static NichtGefundenException keineVersion(Vorlage vorlage, VersionsAngabe angabe, Instant t) {
        return new NichtGefundenException("KEINE_VERSION", "Keine veröffentlichte Version von " + vorlage.vollerName()
                + (angabe == null ? "" : " passend zu " + angabe) + " zum " + t);
    }
}
