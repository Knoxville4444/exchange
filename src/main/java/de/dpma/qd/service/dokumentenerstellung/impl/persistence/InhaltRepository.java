package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dpma.qd.service.dokumentenerstellung.impl.Inhaltsformat;

/** Spring-Data-Repository für die Metadaten der Inhalte. */
public interface InhaltRepository extends JpaRepository<InhaltEntity, Long> {

    Optional<InhaltEntity> findBySha256AndFormat(String sha256, Inhaltsformat format);
}
