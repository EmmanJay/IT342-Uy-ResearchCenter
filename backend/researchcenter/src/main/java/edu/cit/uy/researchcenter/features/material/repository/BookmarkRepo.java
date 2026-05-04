package edu.cit.uy.researchcenter.features.material.repository;

import edu.cit.uy.researchcenter.features.material.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepo extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndMaterialId(Long userId, Long materialId);
    void deleteByUserIdAndMaterialId(Long userId, Long materialId);
    boolean existsByUserIdAndMaterialId(Long userId, Long materialId);
    void deleteByMaterialId(Long materialId);
}
