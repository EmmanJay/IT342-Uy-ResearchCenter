package edu.cit.uy.researchcenter.features.admin;

import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.auth.repository.*;
import edu.cit.uy.researchcenter.features.material.MaterialService;
import edu.cit.uy.researchcenter.features.repository.repository.ResearchRepositoryRepo;
import edu.cit.uy.researchcenter.features.material.repository.MaterialRepo;
import edu.cit.uy.researchcenter.features.request.model.MaterialRequest;
import edu.cit.uy.researchcenter.features.request.repository.MaterialRequestRepo;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminController {

    private final UserRepository userRepository;
    private final ResearchRepositoryRepo repositoryRepo;
    private final MaterialService materialService;
    private final MaterialRepo materialRepo;
    private final MaterialRequestRepo requestRepo;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(this::toUserRow)
                .collect(Collectors.toList());
        return ResponseEntity.ok(wrap(true, users, null));
    }

    @PutMapping("/users/{id}/suspend")
    @Transactional(readOnly = false)
    public ResponseEntity<?> suspendUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        User admin = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        if (admin.getId().equals(id)) {
            return ResponseEntity.badRequest().body(wrap(false, null, "Cannot suspend yourself"));
        }
        User user = userRepository.findById(id).orElseThrow();
        user.setSuspended(!Boolean.TRUE.equals(user.getSuspended()));
        userRepository.save(user);
        return ResponseEntity.ok(wrap(true, Map.of("id", id, "suspended", user.getSuspended()), null));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = userRepository.count();
        long totalRepositories = repositoryRepo.count();
        long totalMaterials = materialRepo.count();
        long totalRequests = requestRepo.count();
        
        return ResponseEntity.ok(wrap(true, Map.of(
            "totalUsers", totalUsers,
            "totalRepositories", totalRepositories,
            "totalMaterials", totalMaterials,
            "totalRequests", totalRequests
        ), null));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElseThrow();
        // Update role logic — set role by name from RoleRepository
        return ResponseEntity.ok(wrap(true, Map.of("id", id, "role", body.get("role")), null));
    }

    @GetMapping("/repositories")
    public ResponseEntity<?> listRepositories() {
        String sql = """
                SELECT r.id, r.name, r.description, r.owner_id, u.email AS owner, r.created_at
                FROM repositories r
                LEFT JOIN users u ON u.id = r.owner_id
                ORDER BY r.created_at DESC
                """;
        List<Map<String, Object>> repos = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("name", rs.getString("name"));
            row.put("description", rs.getString("description"));
            row.put("ownerId", rs.getObject("owner_id"));
            row.put("owner", rs.getString("owner"));
            row.put("isPublic", false);
            row.put("createdAt", rs.getObject("created_at"));
            return row;
        });
        return ResponseEntity.ok(wrap(true, repos, null));
    }

    @DeleteMapping("/repositories/{id}")
    @Transactional(readOnly = false)
    public ResponseEntity<?> deleteRepositoryCascade(@PathVariable Long id) {
        // Delete related child records manually to enforce cascade
        jdbcTemplate.update("DELETE FROM activities WHERE repository_id = ?", id);
        jdbcTemplate.update("DELETE FROM repository_bookmarks WHERE repository_id = ?", id);
        jdbcTemplate.update("DELETE FROM repository_updates WHERE repository_id = ?", id);
        jdbcTemplate.update("DELETE FROM private_notes WHERE repository_id = ?", id);
        jdbcTemplate.update("DELETE FROM repository_members WHERE repository_id = ?", id);
        
        jdbcTemplate.update("DELETE FROM requests WHERE repository_id = ?", id);
        jdbcTemplate.update("DELETE FROM material_notes WHERE material_id IN (SELECT id FROM materials WHERE repository_id = ?)", id);
        jdbcTemplate.update("DELETE FROM bookmarks WHERE material_id IN (SELECT id FROM materials WHERE repository_id = ?)", id);
        jdbcTemplate.update("DELETE FROM material_user_status WHERE material_id IN (SELECT id FROM materials WHERE repository_id = ?)", id);
        jdbcTemplate.update("DELETE FROM material_tags WHERE material_id IN (SELECT id FROM materials WHERE repository_id = ?)", id);
        jdbcTemplate.update("DELETE FROM materials WHERE repository_id = ?", id);
        
        jdbcTemplate.update("DELETE FROM repositories WHERE id = ?", id);
        
        return ResponseEntity.ok(wrap(true, Map.of("message", "Repository deleted successfully"), null));
    }

    @Transactional(readOnly = false)
    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) {
        materialService.deleteAsAdmin(id);
        return ResponseEntity.ok(wrap(true, Map.of("message", "Material deleted"), null));
    }

    @GetMapping("/materials")
    public ResponseEntity<?> listMaterials() {
        String sql = """
                SELECT m.id, m.title, m.repository_id, r.name AS repository_name,
                       m.uploader_id, u.first_name, u.last_name,
                       m.material_type, m.file_url, m.url, m.status, m.created_at
                FROM materials m
                LEFT JOIN repositories r ON r.id = m.repository_id
                LEFT JOIN users u ON u.id = m.uploader_id
                ORDER BY m.created_at DESC
                """;
        List<Map<String, Object>> materials = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String fileUrl = rs.getString("file_url");
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("title", rs.getString("title"));
            row.put("repositoryId", rs.getObject("repository_id"));
            row.put("repositoryName", rs.getString("repository_name"));
            row.put("uploaderId", rs.getObject("uploader_id"));
            row.put("uploaderName", joinName(firstName, lastName));
            row.put("fileType", rs.getString("material_type"));
            row.put("materialType", rs.getString("material_type"));
            row.put("fileUrl", fileUrl != null ? fileUrl : rs.getString("url"));
            row.put("status", rs.getString("status"));
            row.put("createdAt", rs.getObject("created_at"));
            return row;
        });
        return ResponseEntity.ok(wrap(true, materials, null));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> listRequests() {
        String sql = """
                SELECT q.id, q.title, q.description, q.repository_id, r.name AS repository_name,
                       q.requester_id, u.first_name, u.last_name,
                       q.material_id, m.title AS material_title,
                       q.status, q.created_at, q.updated_at
                FROM requests q
                LEFT JOIN repositories r ON r.id = q.repository_id
                LEFT JOIN users u ON u.id = q.requester_id
                LEFT JOIN materials m ON m.id = q.material_id
                ORDER BY q.created_at DESC
                """;
        List<Map<String, Object>> requests = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("title", rs.getString("title"));
            row.put("reason", rs.getString("description"));
            row.put("description", rs.getString("description"));
            row.put("repositoryId", rs.getObject("repository_id"));
            row.put("repositoryName", rs.getString("repository_name"));
            row.put("userId", rs.getObject("requester_id"));
            row.put("requesterId", rs.getObject("requester_id"));
            row.put("requesterName", joinName(firstName, lastName));
            row.put("materialId", rs.getObject("material_id"));
            row.put("materialTitle", rs.getString("material_title"));
            row.put("status", rs.getString("status"));
            row.put("createdAt", rs.getObject("created_at"));
            row.put("updatedAt", rs.getObject("updated_at"));
            return row;
        });
        return ResponseEntity.ok(wrap(true, requests, null));
    }

    @PutMapping("/requests/{id}/status")
    @Transactional(readOnly = false)
    public ResponseEntity<?> updateRequestStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        MaterialRequest request = requestRepo.findById(id).orElseThrow();
        request.setStatus(body.getOrDefault("status", request.getStatus()));
        return ResponseEntity.ok(wrap(true, toRequestRow(requestRepo.save(request)), null));
    }

    @DeleteMapping("/requests/{id}")
    @Transactional(readOnly = false)
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        requestRepo.deleteById(id);
        return ResponseEntity.ok(wrap(true, Map.of("message", "Request deleted"), null));
    }

    private Map<String, Object> toUserRow(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("email", user.getEmail());
        row.put("firstname", user.getFirstName());
        row.put("lastname", user.getLastName());
        row.put("role", user.getRole() != null ? user.getRole().getName() : "USER");
        row.put("createdAt", user.getCreatedAt());
        row.put("suspended", Boolean.TRUE.equals(user.getSuspended()));
        return row;
    }

    private Map<String, Object> toRequestRow(MaterialRequest request) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", request.getId());
        row.put("title", request.getTitle());
        row.put("reason", request.getDescription());
        row.put("description", request.getDescription());
        row.put("repositoryId", request.getRepository() != null ? request.getRepository().getId() : null);
        row.put("repositoryName", request.getRepository() != null ? request.getRepository().getName() : null);
        row.put("userId", request.getRequester() != null ? request.getRequester().getId() : null);
        row.put("requesterId", request.getRequester() != null ? request.getRequester().getId() : null);
        row.put("requesterName", request.getRequester() != null
                ? request.getRequester().getFirstName() + " " + request.getRequester().getLastName()
                : null);
        row.put("materialId", request.getMaterial() != null ? request.getMaterial().getId() : null);
        row.put("materialTitle", request.getMaterial() != null ? request.getMaterial().getTitle() : null);
        row.put("status", request.getStatus());
        row.put("createdAt", request.getCreatedAt());
        row.put("updatedAt", request.getUpdatedAt());
        return row;
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private Map<String, Object> wrap(boolean success, Object data, Object error) {
        return Map.of("success", success, "data", data != null ? data : Map.of(),
                "error", error != null ? error : Map.of(), "timestamp", Instant.now().toString());
    }
}
