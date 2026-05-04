package edu.cit.uy.researchcenter.features.material.repository;

import edu.cit.uy.researchcenter.features.material.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialRepo extends JpaRepository<Material, Long> {
    List<Material> findByRepositoryId(Long repositoryId);
}
