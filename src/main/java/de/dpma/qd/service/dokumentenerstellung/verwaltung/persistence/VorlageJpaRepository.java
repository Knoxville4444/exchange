package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface VorlageJpaRepository extends JpaRepository<VorlageEntity, Long>, JpaSpecificationExecutor<VorlageEntity> {

    @Query("select v from VorlageEntity v where v.kategorie.name = :kategorie and v.name = :name")
    Optional<VorlageEntity> finde(@Param("kategorie") String kategorie, @Param("name") String name);

    /**
     * Sperrt nur die Zeile in DE_VORLAGE ({@code SELECT … FOR UPDATE} ohne Join), damit nicht versehentlich
     * die Kategorie-Zeile mitgesperrt und so alle Änderungen einer Kategorie serialisiert werden.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VorlageEntity v where v.id = :id")
    Optional<VorlageEntity> findeMitSperre(@Param("id") Long id);

    @Query("""
            select distinct v from VorlageEntity v
              join v.versionen ver
              join ver.bausteinReferenzen r
             where r.baustein.id = :bausteinId
            """)
    List<VorlageEntity> findeReferenzierende(@Param("bausteinId") Long bausteinId);

    @Query("select v.kategorie.name, count(v) from VorlageEntity v group by v.kategorie.name")
    List<Object[]> anzahlJeKategorie();
}
