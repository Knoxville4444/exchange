package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "KATEGORIE")
public class KategorieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqKategorie")
    @SequenceGenerator(name = "seqKategorie", sequenceName = "SEQ_KATEGORIE", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "BESCHREIBUNG", length = 2000)
    private String beschreibung;

    @Embedded
    private AuditEmbeddable audit;

    @Version
    @Column(name = "REVISION", nullable = false)
    private Long revision;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public AuditEmbeddable getAudit() {
        return audit;
    }

    public void setAudit(AuditEmbeddable audit) {
        this.audit = audit;
    }

    public Long getRevision() {
        return revision;
    }
}
