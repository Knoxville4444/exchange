package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.VorlagenTyp;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "DE_VORLAGE", uniqueConstraints = @UniqueConstraint(name = "DE_VORLAGE_UK_NAME", columnNames = {"KATEGORIE_ID", "NAME"}))
public class VorlageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deVorlageSeq")
    @SequenceGenerator(name = "deVorlageSeq", sequenceName = "DE_VORLAGE_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "KATEGORIE_ID", nullable = false)
    private KategorieEntity kategorie;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYP", nullable = false, length = 20)
    private VorlagenTyp typ;

    @Column(name = "BEZEICHNUNG", nullable = false, length = 255)
    private String bezeichnung;

    @Column(name = "BESCHREIBUNG", length = 2000)
    private String beschreibung;

    @ElementCollection
    @CollectionTable(name = "DE_VORLAGE_TAG", joinColumns = @JoinColumn(name = "VORLAGE_ID"))
    @Column(name = "TAG", nullable = false, length = 100)
    private Set<String> tags = new HashSet<>();

    @Column(name = "VEROEFF_AB")
    private Instant veroeffentlichtAb;

    @Column(name = "VEROEFF_BIS")
    private Instant veroeffentlichtBis;

    @Embedded
    private AuditEmbeddable audit;

    @Version
    @Column(name = "REVISION", nullable = false)
    private Long revision;

    @OneToMany(mappedBy = "vorlage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VersionEntity> versionen = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public KategorieEntity getKategorie() {
        return kategorie;
    }

    public void setKategorie(KategorieEntity kategorie) {
        this.kategorie = kategorie;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VorlagenTyp getTyp() {
        return typ;
    }

    public void setTyp(VorlagenTyp typ) {
        this.typ = typ;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public Set<String> getTags() {
        return tags;
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

    public AuditEmbeddable getAudit() {
        return audit;
    }

    public void setAudit(AuditEmbeddable audit) {
        this.audit = audit;
    }

    public Long getRevision() {
        return revision;
    }

    public List<VersionEntity> getVersionen() {
        return versionen;
    }
}
