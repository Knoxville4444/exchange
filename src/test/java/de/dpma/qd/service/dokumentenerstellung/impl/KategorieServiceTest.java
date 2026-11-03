package de.dpma.qd.service.dokumentenerstellung.impl;

import static de.dpma.qd.service.dokumentenerstellung.impl.Testdaten.T0;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieEntity;
import de.dpma.qd.service.dokumentenerstellung.impl.persistence.KategorieRepository;

@ExtendWith(MockitoExtension.class)
class KategorieServiceTest implements WithAssertions {

    @Mock
    private KategorieRepository kategorieRepository;

    private KategorieService service;

    @BeforeEach
    void setUp() {
        service = new KategorieService(kategorieRepository, new AuditErzeuger(Clock.fixed(T0, ZoneOffset.UTC)));
    }

    @Nested
    class Speichere {

        @Test
        void legtNeueKategorieAn() {
            when(kategorieRepository.findByName("PATGBM")).thenReturn(Optional.empty());
            when(kategorieRepository.saveAndFlush(any(KategorieEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kategorieRepository.zaehleVorlagenJeKategorie()).thenReturn(List.of());

            KategorieAnsicht ansicht = service.speichere("PATGBM", "Patente und Gebrauchsmuster");

            assertThat(ansicht.kategorie().beschreibung()).isEqualTo("Patente und Gebrauchsmuster");
            assertThat(ansicht.kategorie().audit().erstelltAm()).isEqualTo(T0);
            assertThat(ansicht.anzahlVorlagen()).isEqualTo(0L);
        }
    }

    @Nested
    class Loesche {

        @Test
        void kategorieMitVorlagenIstNichtLoeschbar() {
            KategorieEntity entity = new KategorieEntity();
            entity.setName("PATGBM");
            when(kategorieRepository.findByName("PATGBM")).thenReturn(Optional.of(entity));
            when(kategorieRepository.zaehleVorlagenJeKategorie()).thenReturn(List.<Object[]>of(new Object[] {"PATGBM", 3L}));

            assertThatThrownBy(() -> service.loesche("PATGBM"))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.errorId()).isEqualTo("KATEGORIE_NICHT_LEER"));
            verify(kategorieRepository, never()).delete(any(KategorieEntity.class));
        }

        @Test
        void unbekannteKategorieWirdGemeldet() {
            when(kategorieRepository.findByName("XYZ")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loesche("XYZ"))
                    .isInstanceOfSatisfying(FachlicherFehler.class, f -> assertThat(f.art()).isEqualTo(FachlicherFehler.Art.NICHT_GEFUNDEN));
        }
    }
}
