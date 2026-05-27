package edu.cit.uy.researchcenter.features.activity.repository;

import edu.cit.uy.researchcenter.features.activity.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Activity> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId, Pageable pageable);

    // Recent activity for a specific user (only their actions)
    Page<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Repo Activity excluding private notes
    Page<Activity> findByRepositoryIdAndTargetTypeNotOrderByCreatedAtDesc(Long repositoryId, String targetType, Pageable pageable);

    // Notifications for a user (actions targeting them, OR actions in their repos done by OTHERS)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Activity a WHERE (a.targetUserId = :userId OR a.repositoryId IN (SELECT rm.repository.id FROM RepositoryMember rm WHERE rm.user.id = :userId)) AND (a.userId != :userId OR a.userId IS NULL) ORDER BY a.createdAt DESC")
    Page<Activity> findNotificationsForUser(@org.springframework.data.repository.query.Param("userId") Long userId, Pageable pageable);

    // Admin notifications (all actions EXCEPT the admin's own actions)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Activity a WHERE (a.userId != :adminId OR a.userId IS NULL) ORDER BY a.createdAt DESC")
    Page<Activity> findAllAdminNotifications(@org.springframework.data.repository.query.Param("adminId") Long adminId, Pageable pageable);
}
