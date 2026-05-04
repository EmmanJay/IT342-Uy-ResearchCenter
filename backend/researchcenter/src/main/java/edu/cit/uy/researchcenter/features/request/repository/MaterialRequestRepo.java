package edu.cit.uy.researchcenter.features.request.repository;

import edu.cit.uy.researchcenter.features.request.model.MaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialRequestRepo extends JpaRepository<MaterialRequest, Long> {
    List<MaterialRequest> findByRepositoryId(Long repositoryId);
    List<MaterialRequest> findByRepositoryIdAndStatus(Long repositoryId, String status);
    List<MaterialRequest> findByMaterialId(Long materialId);
    List<MaterialRequest> findByRequesterId(Long requesterId);
}
