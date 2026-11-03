package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Spring-Data-Repository für Vorlagen samt Versionen und Baustein-Referenzen. */
public interface VorlageRepository extends JpaRepository<VorlageEntity, Long>, JpaSpecificationExecutor<VorlageEntity> {

    @Query("select v from VorlageEntity v where v.kategorie.name = :kategorie and v.name = :name")
    Optional<VorlageEntity> finde(@Param("kategorie") String kategorie, @Param("name") String name);

    /**
     * Sperrt nur die Zeile in VORLAGE ({@code SELECT … FOR UPDATE} ohne Join), damit nicht die Kategorie-Zeile mitgesperrt
     * und so jede Änderung innerhalb einer Kategorie serialisiert wird.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VorlageEntity v where v.id = :id")
    Optional<VorlageEntity> findeMitSperre(@Param("id") Long id);

    /** Lädt eine Vorlage mit Schreibsperre; Pflicht für alle ändernden Operationen (z. B. Vergabe der Versionsnummer). */
    default Optional<VorlageEntity> findeZumAendern(String kategorie, String name) {
        return finde(kategorie, name).flatMap(e -> findeMitSperre(e.getId()));
    }

    /** Alle Formulare, von denen mindestens eine Version den Baustein referenziert. */
    @Query("""
            select distinct v from VorlageEntity v
              join v.versionen ver
              join ver.bausteinReferenzen r
             where r.baustein.kategorie.name = :kategorie and r.baustein.name = :name
            """)
    List<VorlageEntity> findeReferenzierende(@Param("kategorie") String kategorie, @Param("name") String name);
}
