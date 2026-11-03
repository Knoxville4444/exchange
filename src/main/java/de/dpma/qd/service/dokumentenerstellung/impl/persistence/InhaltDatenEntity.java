package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Binärdaten eines Inhalts (BLOB). Die ID entspricht der ID in {@link InhaltEntity}. */
@Entity
@Table(name = "INHALT_DATEN")
public class InhaltDatenEntity {

    @Id
    @Column(name = "INHALT_ID")
    private Long inhaltId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "DATEN", nullable = false)
    private byte[] daten;

    protected InhaltDatenEntity() {
    }

    public InhaltDatenEntity(Long inhaltId, byte[] daten) {
        this.inhaltId = inhaltId;
        this.daten = daten;
    }

    public byte[] getDaten() {
        return daten;
    }
}
