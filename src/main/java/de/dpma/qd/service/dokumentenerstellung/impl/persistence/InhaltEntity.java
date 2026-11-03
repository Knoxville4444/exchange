package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import java.time.Instant;

import de.dpma.qd.service.dokumentenerstellung.impl.Inhaltsformat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Metadaten eines Inhalts. Die Daten selbst liegen in {@link InhaltDatenEntity} (eigene Tabelle), damit beim
 * Laden von Versionen nie BLOBs mitgelesen werden.
 */
@Entity
@Table(name = "INHALT", uniqueConstraints = @UniqueConstraint(name = "INHALT_UK_SHA", columnNames = {"SHA256", "FORMAT"}))
public class InhaltEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqInhalt")
    @SequenceGenerator(name = "seqInhalt", sequenceName = "SEQ_INHALT", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SHA256", nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "FORMAT", nullable = false, length = 10)
    private Inhaltsformat format;

    @Column(name = "GROESSE", nullable = false)
    private long groesse;

    @Column(name = "ERSTELLT_TIME", nullable = false)
    private Instant erstelltAm;

    protected InhaltEntity() {
    }

    public InhaltEntity(String sha256, Inhaltsformat format, long groesse, Instant erstelltAm) {
        this.sha256 = sha256;
        this.format = format;
        this.groesse = groesse;
        this.erstelltAm = erstelltAm;
    }

    public Long getId() {
        return id;
    }

    public String getSha256() {
        return sha256;
    }

    public Inhaltsformat getFormat() {
        return format;
    }

    public long getGroesse() {
        return groesse;
    }
}
