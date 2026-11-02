package de.dpma.qd.service.dokumentenerstellung.verwaltung.domain;

/** Typ einer Vorlage; wird beim Anlegen festgelegt und ist nicht änderbar. */
public enum VorlagenTyp {
    /** ODT mit Variablen und Bausteinen, Erzeugung als ODT oder PDF/A. */
    FORMULAR(Inhaltsformat.ODT),
    /** ODT-Fragment, wird in Formulare eingebunden. */
    BAUSTEIN(Inhaltsformat.ODT),
    /** PDF, wird 1:1 ausgeliefert. */
    MERKBLATT(Inhaltsformat.PDF);

    private final Inhaltsformat inhaltsformat;

    VorlagenTyp(Inhaltsformat inhaltsformat) {
        this.inhaltsformat = inhaltsformat;
    }

    public Inhaltsformat inhaltsformat() {
        return inhaltsformat;
    }

    /** Nur Formulare haben ein XML-Schema (XSD) und Baustein-Referenzen. */
    public boolean istFormular() {
        return this == FORMULAR;
    }
}
