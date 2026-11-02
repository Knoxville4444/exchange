package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class BausteinReferenzEmbeddable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BAUSTEIN_VORLAGE_ID", nullable = false)
    private VorlageEntity baustein;

    @Column(name = "ANGABE_HAUPT", nullable = false)
    private int angabeHaupt;

    @Column(name = "ANGABE_NEBEN")
    private Integer angabeNeben;

    protected BausteinReferenzEmbeddable() {
    }

    public BausteinReferenzEmbeddable(VorlageEntity baustein, int angabeHaupt, Integer angabeNeben) {
        this.baustein = baustein;
        this.angabeHaupt = angabeHaupt;
        this.angabeNeben = angabeNeben;
    }

    public VorlageEntity getBaustein() {
        return baustein;
    }

    public int getAngabeHaupt() {
        return angabeHaupt;
    }

    public Integer getAngabeNeben() {
        return angabeNeben;
    }
}
