package edu.cit.uy.researchcenter.features.repository.repository;

import edu.cit.uy.researchcenter.features.repository.model.RepositoryBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RepositoryBookmarkRepository extends JpaRepository<RepositoryBookmark, Long> {
    Optional<RepositoryBookmark> findByUserIdAndRepositoryId(Long userId, Long repositoryId);
    List<RepositoryBookmark> findByUserId(Long userId);
    void deleteByUserIdAndRepositoryId(Long userId, Long repositoryId);
    boolean existsByUserIdAndRepositoryId(Long userId, Long repositoryId);
}
