package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.InhaltRepository;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageEntityMapper;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.VorlageRepository;

/**
 * Veröffentlichung planen, verschieben, absagen und zurückziehen – je Version oder für die gesamte Vorlage.
 * <p>
 * Der effektive Status wird aus Status und Zeitraum berechnet; es gibt keinen Scheduler. Änderungen wirken nur in die Zukunft, damit
 * frühere Stände rekonstruierbar bleiben. Formulare und Bausteine werden gegeneinander geprüft ({@link BausteinService}).
 */
@Service
@Transactional
public class VeroeffentlichungService {

    private final VorlageRepository vorlagen;
    private final InhaltRepository inhaltRepository;
    private final BausteinService bausteine;
    private final AuditErzeuger audit;

    public VeroeffentlichungService(VorlageRepository vorlagen, InhaltRepository inhaltRepository, BausteinService bausteine,
            AuditErzeuger audit) {
        this.vorlagen = vorlagen;
        this.inhaltRepository = inhaltRepository;
        this.bausteine = bausteine;
        this.audit = audit;
    }

    /**
     * Veröffentlicht eine Version sofort oder geplant, verschiebt den Termin oder zieht sie zurück.
     *
     * @param ab  {@code null} = jetzt bzw. unverändert bei bereits veröffentlichten Versionen
     * @param bis {@code null} = unbefristet
     */
    public VersionsAnsicht setzeVersion(String kategorie, String name, VersionsNummer nummer, Instant ab, Instant bis) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        Version version = vorlage.version(nummer);
        Instant jetzt = audit.jetzt();
        Zeitraum neu = neuerZeitraum(version, ab, bis, jetzt);
        Vorlage geaendert = vorlage.mitVersion(version.freigegeben(neu, audit.geaendert(version.audit())));
        return pruefenUndSpeichern(entity, geaendert, nummer);
    }

    /** Sagt eine geplante Veröffentlichung ab; die Version wird wieder DRAFT. */
    public VersionsAnsicht sageVersionAb(String kategorie, String name, VersionsNummer nummer) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        Version version = vorlage.version(nummer);
        if (version.effektiverStatus(audit.jetzt()) != EffektiverVersionStatus.GEPLANT) {
            throw FachlicherFehler.konflikt("VERSION_NICHT_GEPLANT", "Nur geplante Veröffentlichungen können abgesagt werden (Version " + nummer + ")");
        }
        return pruefenUndSpeichern(entity, vorlage.mitVersion(version.alsEntwurf(audit.geaendert(version.audit()))), nummer);
    }

    /** Setzt den Zeitraum der gesamten Vorlage; {@code null} bzw. ein leerer Zeitraum entfernt ihn. */
    public VorlagenAnsicht setzeVorlage(String kategorie, String name, Zeitraum zeitraum) {
        VorlageEntity entity = ladeZumAendern(kategorie, name);
        Vorlage vorlage = VorlageEntityMapper.zuFachobjekt(entity);
        Zeitraum bereinigt = zeitraum == null || zeitraum.ab() == null && zeitraum.bis() == null ? null : zeitraum;
        Instant jetzt = audit.jetzt();
        if (!Zeitraum.gleicheVergangenheit(vorlage.veroeffentlichung(), bereinigt, jetzt)) {
            throw FachlicherFehler.konflikt("VERGANGENHEIT_UNVERAENDERLICH", "Der Zeitraum darf nur mit Wirkung für die Zukunft geändert werden");
        }
        Vorlage geaendert = vorlage.mitZeitraum(bereinigt, audit.geaendert(vorlage.audit()));
        bausteine.pruefeFormular(geaendert);
        bausteine.pruefeBausteinAenderung(geaendert);
        VorlageEntityMapper.uebertrage(geaendert, entity, vorlagen, inhaltRepository);
        return new VorlagenAnsicht(VorlageEntityMapper.zuFachobjekt(vorlagen.saveAndFlush(entity)), jetzt);
    }

    /**
     * Zulässige Änderungen je effektivem Status: DRAFT und GEPLANT frei (ab nicht in der Vergangenheit), VEROEFFENTLICHT nur noch
     * {@code bis}, ZURUECKGEZOGEN gar nicht.
     */
    private static Zeitraum neuerZeitraum(Version version, Instant ab, Instant bis, Instant jetzt) {
        if (bis != null && bis.isBefore(jetzt)) {
            throw FachlicherFehler.ungueltig("BIS_IN_VERGANGENHEIT", "bis darf nicht in der Vergangenheit liegen");
        }
        return switch (version.effektiverStatus(jetzt)) {
            case DRAFT, GEPLANT -> {
                Instant neuesAb = ab == null ? jetzt : ab;
                if (neuesAb.isBefore(jetzt)) {
                    throw FachlicherFehler.ungueltig("AB_IN_VERGANGENHEIT",
                            "ab darf nicht in der Vergangenheit liegen; für eine sofortige Veröffentlichung ab weglassen");
                }
                yield new Zeitraum(neuesAb, bis);
            }
            case VEROEFFENTLICHT -> {
                if (ab != null && !ab.equals(version.zeitraum().ab())) {
                    throw FachlicherFehler.konflikt("AB_UNVERAENDERLICH", "Version " + version.nummer() + " ist veröffentlicht; ab ist nicht mehr änderbar");
                }
                yield new Zeitraum(version.zeitraum().ab(), bis);
            }
            case ZURUECKGEZOGEN -> throw FachlicherFehler.konflikt("VERSION_ZURUECKGEZOGEN",
                    "Version " + version.nummer() + " ist zurückgezogen und nicht mehr änderbar");
        };
    }

    private VersionsAnsicht pruefenUndSpeichern(VorlageEntity entity, Vorlage geaendert, VersionsNummer nummer) {
        bausteine.pruefeFormular(geaendert);
        bausteine.pruefeBausteinAenderung(geaendert);
        VorlageEntityMapper.uebertrage(geaendert, entity, vorlagen, inhaltRepository);
        Vorlage gespeichert = VorlageEntityMapper.zuFachobjekt(vorlagen.saveAndFlush(entity));
        return bausteine.ansicht(gespeichert, gespeichert.version(nummer), audit.jetzt());
    }

    private VorlageEntity ladeZumAendern(String kategorie, String name) {
        return vorlagen.findeZumAendern(kategorie, name).orElseThrow(() -> FachlicherFehler.vorlageNichtGefunden(kategorie, name));
    }
}
