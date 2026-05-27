package edu.cit.uy.researchcenter.features.repository.repository;

import edu.cit.uy.researchcenter.features.repository.model.RepositoryUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryUpdateRepository extends JpaRepository<RepositoryUpdate, Long> {
    Page<RepositoryUpdate> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId, Pageable pageable);
}
