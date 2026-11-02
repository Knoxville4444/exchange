package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VersionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DE_VERSION", uniqueConstraints = @UniqueConstraint(name = "DE_VERSION_UK_NUMMER", columnNames = {"VORLAGE_ID", "HAUPT", "NEBEN"}))
public class VersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deVersionSeq")
    @SequenceGenerator(name = "deVersionSeq", sequenceName = "DE_VERSION_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "VORLAGE_ID", nullable = false)
    private VorlageEntity vorlage;

    @Column(name = "HAUPT", nullable = false)
    private int haupt;

    @Column(name = "NEBEN", nullable = false)
    private int neben;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private VersionStatus status;

    @Column(name = "VEROEFF_AB")
    private Instant veroeffentlichtAb;

    @Column(name = "VEROEFF_BIS")
    private Instant veroeffentlichtBis;

    @Column(name = "KOMMENTAR", length = 2000)
    private String kommentar;

    @ManyToOne(optional = false)
    @JoinColumn(name = "INHALT_ID", nullable = false)
    private InhaltEntity inhalt;

    @ManyToOne
    @JoinColumn(name = "SCHEMA_ID")
    private InhaltEntity schema;

    @ElementCollection
    @CollectionTable(name = "DE_BAUSTEIN_REF", joinColumns = @JoinColumn(name = "VERSION_ID"))
    private List<BausteinReferenzEmbeddable> bausteinReferenzen = new ArrayList<>();

    @Embedded
    private AuditEmbeddable audit;

    public Long getId() {
        return id;
    }

    public VorlageEntity getVorlage() {
        return vorlage;
    }

    public void setVorlage(VorlageEntity vorlage) {
        this.vorlage = vorlage;
    }

    public int getHaupt() {
        return haupt;
    }

    public void setHaupt(int haupt) {
        this.haupt = haupt;
    }

    public int getNeben() {
        return neben;
    }

    public void setNeben(int neben) {
        this.neben = neben;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public Instant getVeroeffentlichtAb() {
        return veroeffentlichtAb;
    }

    public void setVeroeffentlichtAb(Instant veroeffentlichtAb) {
        this.veroeffentlichtAb = veroeffentlichtAb;
    }

    public Instant getVeroeffentlichtBis() {
        return veroeffentlichtBis;
    }

    public void setVeroeffentlichtBis(Instant veroeffentlichtBis) {
        this.veroeffentlichtBis = veroeffentlichtBis;
    }

    public String getKommentar() {
        return kommentar;
    }

    public void setKommentar(String kommentar) {
        this.kommentar = kommentar;
    }

    public InhaltEntity getInhalt() {
        return inhalt;
    }

    public void setInhalt(InhaltEntity inhalt) {
        this.inhalt = inhalt;
    }

    public InhaltEntity getSchema() {
        return schema;
    }

    public void setSchema(InhaltEntity schema) {
        this.schema = schema;
    }

    public List<BausteinReferenzEmbeddable> getBausteinReferenzen() {
        return bausteinReferenzen;
    }

    public AuditEmbeddable getAudit() {
        return audit;
    }

    public void setAudit(AuditEmbeddable audit) {
        this.audit = audit;
    }
}
