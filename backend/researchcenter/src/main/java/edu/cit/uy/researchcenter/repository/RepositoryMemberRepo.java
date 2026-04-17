package edu.cit.uy.researchcenter.repository;

import edu.cit.uy.researchcenter.model.RepositoryMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryMemberRepo extends JpaRepository<RepositoryMember, Long> {

    List<RepositoryMember> findByRepositoryId(Long repositoryId);

    Optional<RepositoryMember> findByRepositoryIdAndUserId(Long repositoryId, Long userId);

    boolean existsByRepositoryIdAndUserId(Long repositoryId, Long userId);

    // All repos a user is a member of (not owner)
    @Query("SELECT rm FROM RepositoryMember rm WHERE rm.user.id = :userId")
    List<RepositoryMember> findAllByUserId(Long userId);

    void deleteByRepositoryIdAndUserId(Long repositoryId, Long userId);
}
