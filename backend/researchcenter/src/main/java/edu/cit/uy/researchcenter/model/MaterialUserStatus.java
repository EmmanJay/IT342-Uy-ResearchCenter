package edu.cit.uy.researchcenter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "material_user_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUserStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String status;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() { updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
