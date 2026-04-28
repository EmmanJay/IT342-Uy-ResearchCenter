package edu.cit.uy.researchcenter.repository;

import edu.cit.uy.researchcenter.model.MaterialUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialUserStatusRepo extends JpaRepository<MaterialUserStatus, Long> {
    Optional<MaterialUserStatus> findByMaterialIdAndUserId(Long materialId, Long userId);
    void deleteByMaterialId(Long materialId);
}
