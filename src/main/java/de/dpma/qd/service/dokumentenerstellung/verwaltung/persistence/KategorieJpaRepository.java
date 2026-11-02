package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface KategorieJpaRepository extends JpaRepository<KategorieEntity, Long> {

    Optional<KategorieEntity> findByName(String name);
}
