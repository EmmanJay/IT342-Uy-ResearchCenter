package edu.cit.uy.researchcenter.features.repository.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "repository_bookmarks")
public class RepositoryBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
