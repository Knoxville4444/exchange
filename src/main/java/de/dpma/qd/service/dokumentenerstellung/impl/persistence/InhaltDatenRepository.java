package de.dpma.qd.service.dokumentenerstellung.impl.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring-Data-Repository für die Binärdaten der Inhalte (BLOB). */
public interface InhaltDatenRepository extends JpaRepository<InhaltDatenEntity, Long> {
}
