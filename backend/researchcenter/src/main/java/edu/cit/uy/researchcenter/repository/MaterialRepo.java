package edu.cit.uy.researchcenter.repository;

import edu.cit.uy.researchcenter.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialRepo extends JpaRepository<Material, Long> {
    List<Material> findByRepositoryId(Long repositoryId);
}
