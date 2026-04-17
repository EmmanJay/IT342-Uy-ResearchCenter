package edu.cit.uy.researchcenter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private ResearchRepository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "material_type", length = 50)
    private String materialType;  // PDF | LINK | REFERENCE

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(length = 1000)
    private String url;

    @Column(length = 50)
    private String status;  // TO_READ | IN_PROGRESS | COMPLETED

    // Authors / publisher / year for REFERENCE type
    @Column(length = 500)
    private String authors;

    @Column(length = 255)
    private String publisher;

    @Column(length = 10)
    private String year;
    
    @Column(length = 50)
    private String isbn;

    // Store Google Books metadata as JSON string
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaterialTag> tags;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
