package edu.cit.uy.researchcenter.features.repository.repository;

import edu.cit.uy.researchcenter.features.repository.model.ResearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResearchRepositoryRepo extends JpaRepository<ResearchRepository, Long> {
    List<ResearchRepository> findByOwnerId(Long ownerId);
}
