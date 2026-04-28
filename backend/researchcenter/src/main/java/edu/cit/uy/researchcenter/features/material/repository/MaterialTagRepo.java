package edu.cit.uy.researchcenter.features.material.repository;

import edu.cit.uy.researchcenter.features.material.model.MaterialTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialTagRepo extends JpaRepository<MaterialTag, Long> {
    List<MaterialTag> findByMaterialId(Long materialId);
    void deleteByMaterialId(Long materialId);
}
