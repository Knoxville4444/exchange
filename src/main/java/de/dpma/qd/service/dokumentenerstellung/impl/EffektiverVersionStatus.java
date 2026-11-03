package de.dpma.qd.service.dokumentenerstellung.impl;

/** Aus gespeichertem Status und Zeitraum zu einem Zeitpunkt abgeleiteter Status einer Version. */
public enum EffektiverVersionStatus {
    DRAFT,
    GEPLANT,
    VEROEFFENTLICHT,
    ZURUECKGEZOGEN;

    /** Version ist jetzt nutzbar oder wird es künftig sein. */
    public boolean istAktivOderGeplant() {
        return this == GEPLANT || this == VEROEFFENTLICHT;
    }
}
