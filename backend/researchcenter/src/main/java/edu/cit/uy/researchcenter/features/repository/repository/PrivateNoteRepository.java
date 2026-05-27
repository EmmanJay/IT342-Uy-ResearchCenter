package edu.cit.uy.researchcenter.features.repository.repository;

import edu.cit.uy.researchcenter.features.repository.model.PrivateNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PrivateNoteRepository extends JpaRepository<PrivateNote, Long> {
    Optional<PrivateNote> findByUserIdAndRepositoryId(Long userId, Long repositoryId);
}
