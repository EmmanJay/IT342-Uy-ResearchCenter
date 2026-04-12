package edu.cit.uy.researchcenter.service;

import edu.cit.uy.researchcenter.dto.*;
import edu.cit.uy.researchcenter.model.*;
import edu.cit.uy.researchcenter.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private static final Logger logger = LoggerFactory.getLogger(MaterialService.class);
    private final MaterialRepo materialRepo;
    private final MaterialTagRepo tagRepo;
    private final ResearchRepositoryRepo repositoryRepo;
    private final RepositoryMemberRepo memberRepo;
    private final MaterialUserStatusRepo userStatusRepo;
    private final SupabaseStorageService supabaseStorageService;
    private final MaterialRequestRepo requestRepo;
    private final BookmarkRepo bookmarkRepo;

    // ── Add material ──────────────────────────────────────────────────────
    @Transactional
    public MaterialResponse create(User uploader, CreateMaterialRequest req) {
        ResearchRepository repo = repositoryRepo.findById(req.getRepositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

        if (!memberRepo.existsByRepositoryIdAndUserId(repo.getId(), uploader.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }

        Material material = Material.builder()
                .repository(repo)
                .uploader(uploader)
                .title(req.getTitle())
                .description(req.getDescription())
                .materialType(req.getMaterialType())
                .fileUrl(req.getFileUrl())
                .url(req.getUrl())
                .authors(req.getAuthors())
                .publisher(req.getPublisher())
                .year(req.getYear())
                .isbn(req.getIsbn())
                .metadata(req.getMetadata())
                .status(req.getStatus() != null ? req.getStatus() : "TO_READ")
                .build();

        material = materialRepo.save(material);

        // Save tags
        if (req.getTags() != null) {
            final Material savedMaterial = material;
            List<MaterialTag> tags = req.getTags().stream()
                    .filter(t -> !t.isBlank())
                    .map(t -> t.trim().toLowerCase())
                    .distinct()
                    .map(t -> MaterialTag.builder().material(savedMaterial).tagName(t).build())
                    .collect(Collectors.toList());
            tagRepo.saveAll(tags);
        }

        return toResponse(materialRepo.findById(material.getId()).orElseThrow(), uploader.getId());
    }

    @Transactional
    public MaterialResponse update(Long id, Long callerId, CreateMaterialRequest req) {
        Material material = materialRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        if (!material.getUploader().getId().equals(callerId) && !material.getRepository().getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update");
        }

        if (req.getTitle() != null) material.setTitle(req.getTitle());
        if (req.getDescription() != null) material.setDescription(req.getDescription());
        if (req.getFileUrl() != null) material.setFileUrl(req.getFileUrl());
        if (req.getUrl() != null) material.setUrl(req.getUrl());
        if (req.getAuthors() != null) material.setAuthors(req.getAuthors());
        if (req.getPublisher() != null) material.setPublisher(req.getPublisher());
        if (req.getYear() != null) material.setYear(req.getYear());
        if (req.getIsbn() != null) material.setIsbn(req.getIsbn());
        if (req.getMetadata() != null) material.setMetadata(req.getMetadata());

        material = materialRepo.save(material);

        if (req.getTags() != null) {
            tagRepo.deleteByMaterialId(material.getId());
            final Material savedMaterial = material;
            List<MaterialTag> tags = req.getTags().stream()
                    .filter(t -> !t.isBlank())
                    .map(t -> t.trim().toLowerCase())
                    .distinct()
                    .map(t -> MaterialTag.builder().material(savedMaterial).tagName(t).build())
                    .collect(Collectors.toList());
            tagRepo.saveAll(tags);
        }

        return toResponse(material, callerId);
    }

    // ── Get by repo ───────────────────────────────────────────────────────
    public List<MaterialResponse> getByRepo(Long repoId, Long callerId) {
        if (!memberRepo.existsByRepositoryIdAndUserId(repoId, callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }
        return materialRepo.findByRepositoryId(repoId).stream()
                .map(m -> toResponse(m, callerId))
                .collect(Collectors.toList());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────
    public MaterialResponse getById(Long id, Long callerId) {
        Material mat = materialRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));
        if (!memberRepo.existsByRepositoryIdAndUserId(mat.getRepository().getId(), callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }
        return toResponse(mat, callerId);
    }

    // ── Delete ────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, Long callerId) {
        try {
            logger.info("Delete started for material id=" + id + ", caller id=" + callerId);

            Material mat = materialRepo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));
            logger.info("Material found: " + mat.getTitle());

            boolean isOwner = mat.getRepository().getOwner().getId().equals(callerId);
            boolean isUploader = mat.getUploader().getId().equals(callerId);
            logger.info("Authorization check: isOwner=" + isOwner + ", isUploader=" + isUploader);

            if (!isOwner && !isUploader) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the uploader or repository owner can delete this material");
            }

            // Delete stored file from Supabase if present (best-effort)
            if (mat.getFileUrl() != null && !mat.getFileUrl().isBlank()) {
                try {
                    logger.info("Deleting file from Supabase: " + mat.getFileUrl());
                    supabaseStorageService.deleteFile(mat.getFileUrl());
                    logger.info("File deleted from Supabase");
                } catch (Exception e) {
                    logger.warn("Failed to delete file from Supabase", e);
                }
            }

            // Clean up all foreign key references before deleting the material
            // 1. Delete Bookmark records that reference this material
            logger.info("Deleting Bookmark records for material id=" + id);
            bookmarkRepo.deleteByMaterialId(id);
            logger.info("Bookmark records deleted");

            // 2. Delete MaterialUserStatus records that reference this material
            logger.info("Deleting MaterialUserStatus records for material id=" + id);
            userStatusRepo.deleteByMaterialId(id);
            logger.info("MaterialUserStatus records deleted");

            // 3. Detach MaterialRequest records that reference this material
            logger.info("Finding and detaching MaterialRequest records for material id=" + id);
            List<MaterialRequest> requests = requestRepo.findByMaterialId(id);
            logger.info("Found " + requests.size() + " MaterialRequest records");
            for (MaterialRequest req : requests) {
                logger.info("Detaching request id=" + req.getId());
                req.setMaterial(null);
                req.setStatus("CANCELLED");
                requestRepo.save(req);
            }
            logger.info("All MaterialRequest records detached");

            // 4. Delete the material (tags will cascade delete via CascadeType.ALL, orphanRemoval = true)
            logger.info("Deleting material id=" + id);
            materialRepo.deleteById(id);
            logger.info("Material deleted successfully");
        } catch (Exception e) {
            logger.error("Error in delete method for material id=" + id, e);
            throw e;
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────
        private MaterialResponse toResponse(Material m, Long callerId) {
        List<String> tags = tagRepo.findByMaterialId(m.getId()).stream()
                .map(MaterialTag::getTagName).collect(Collectors.toList());
        MaterialResponse.MaterialResponseBuilder builder = MaterialResponse.builder()
                .id(m.getId())
                .repositoryId(m.getRepository().getId())
                .repositoryName(m.getRepository().getName())
                .title(m.getTitle())
                .description(m.getDescription())
            .materialType(m.getMaterialType())
                .fileUrl(m.getFileUrl())
                .url(m.getUrl())
                .authors(m.getAuthors())
                .publisher(m.getPublisher())
                .year(m.getYear())
                .isbn(m.getIsbn())
                .metadata(m.getMetadata())
                .uploaderId(m.getUploader().getId())
            .uploaderName(m.getUploader().getFirstName() + " " + m.getUploader().getLastName())
                .tags(tags)
                .status(m.getStatus())
            .createdAt(m.getCreatedAt())
            .updatedAt(m.getUpdatedAt());

        // include per-user status if available
        if (callerId != null) {
            userStatusRepo.findByMaterialIdAndUserId(m.getId(), callerId)
                .ifPresent(s -> builder.myStatus(s.getStatus()));
        }

        return builder.build();
    }

        @Transactional
        public String updateUserStatus(Long materialId, Long userId, String status) {
            try {
                System.out.println("updateUserStatus called with materialId=" + materialId + ", userId=" + userId + ", status=" + status);
                Material mat = materialRepo.findById(materialId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));
                if (!memberRepo.existsByRepositoryIdAndUserId(mat.getRepository().getId(), userId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
                }

                MaterialUserStatus sus = userStatusRepo.findByMaterialIdAndUserId(materialId, userId)
                    .orElseGet(() -> {
                        User u = new User();
                        u.setId(userId);
                        return MaterialUserStatus.builder().material(mat).user(u).build();
                    });
                sus.setStatus(status);
                MaterialUserStatus saved = userStatusRepo.save(sus);
                System.out.println("updateUserStatus saved id=" + (saved != null ? saved.getId() : "null"));
                return status;
            } catch (Exception e) {
                System.err.println("Exception in updateUserStatus: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
}
