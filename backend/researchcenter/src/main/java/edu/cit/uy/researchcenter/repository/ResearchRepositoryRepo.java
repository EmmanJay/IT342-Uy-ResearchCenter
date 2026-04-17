package edu.cit.uy.researchcenter.repository;

import edu.cit.uy.researchcenter.model.ResearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResearchRepositoryRepo extends JpaRepository<ResearchRepository, Long> {
    List<ResearchRepository> findByOwnerId(Long ownerId);
}
