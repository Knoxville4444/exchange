package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring-Data-Repository für Kategorien. */
public interface KategorieRepository extends JpaRepository<KategorieEntity, Long> {

    Optional<KategorieEntity> findByName(String name);

    @Query("select v.kategorie.name, count(v) from VorlageEntity v group by v.kategorie.name")
    List<Object[]> zaehleVorlagenJeKategorie();
}
