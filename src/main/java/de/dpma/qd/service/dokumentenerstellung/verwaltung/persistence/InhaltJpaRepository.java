package de.dpma.qd.service.dokumentenerstellung.verwaltung.persistence;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Inhaltsformat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface InhaltJpaRepository extends JpaRepository<InhaltEntity, Long> {

    Optional<InhaltEntity> findBySha256AndFormat(String sha256, Inhaltsformat format);
}
