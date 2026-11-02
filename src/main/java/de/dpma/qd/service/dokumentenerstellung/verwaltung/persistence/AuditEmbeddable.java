package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class AuditEmbeddable {

    @Column(name = "ERSTELLT_AM", nullable = false)
    private Instant erstelltAm;

    @Column(name = "ERSTELLT_VON", nullable = false, length = 255)
    private String erstelltVon;

    @Column(name = "GEAENDERT_AM")
    private Instant geaendertAm;

    @Column(name = "GEAENDERT_VON", length = 255)
    private String geaendertVon;

    protected AuditEmbeddable() {
    }

    public AuditEmbeddable(Instant erstelltAm, String erstelltVon, Instant geaendertAm, String geaendertVon) {
        this.erstelltAm = erstelltAm;
        this.erstelltVon = erstelltVon;
        this.geaendertAm = geaendertAm;
        this.geaendertVon = geaendertVon;
    }

    public Instant getErstelltAm() {
        return erstelltAm;
    }

    public String getErstelltVon() {
        return erstelltVon;
    }

    public Instant getGeaendertAm() {
        return geaendertAm;
    }

    public String getGeaendertVon() {
        return geaendertVon;
    }
}
