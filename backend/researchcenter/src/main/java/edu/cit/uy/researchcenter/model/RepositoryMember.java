package edu.cit.uy.researchcenter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "repository_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"repository_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private ResearchRepository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_in_repo", length = 50)
    private String roleInRepo;   // "OWNER" | "MEMBER"

    @Column(name = "joined_at")
    private Instant joinedAt;

    @PrePersist
    void onCreate() { joinedAt = Instant.now(); }
}
