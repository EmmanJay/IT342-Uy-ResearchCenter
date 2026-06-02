package edu.cit.uy.researchcenter.features.material.repository;

import edu.cit.uy.researchcenter.features.material.model.MaterialNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialNoteRepo extends JpaRepository<MaterialNote, Long> {
    Optional<MaterialNote> findByUserIdAndMaterialId(Long userId, Long materialId);
    void deleteByMaterialId(Long materialId);
}
