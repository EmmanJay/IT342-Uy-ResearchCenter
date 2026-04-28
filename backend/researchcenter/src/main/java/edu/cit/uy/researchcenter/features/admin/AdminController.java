package edu.cit.uy.researchcenter.controller;

import edu.cit.uy.researchcenter.model.User;
import edu.cit.uy.researchcenter.repository.*;
import edu.cit.uy.researchcenter.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ResearchRepositoryRepo repositoryRepo;
    private final MaterialService materialService;
    private final MaterialRepo materialRepo;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(), "email", u.getEmail(),
                        "firstname", u.getFirstName(), "lastname", u.getLastName(),
                        "role", u.getRole().getName(), "createdAt", u.getCreatedAt()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(wrap(true, Map.of("users", users, "totalCount", users.size()), null));
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
        List<Map<String, Object>> repos = repositoryRepo.findAll().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(), "name", r.getName(),
                        "owner", r.getOwner().getEmail(), "createdAt", r.getCreatedAt()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(wrap(true, repos, null));
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@AuthenticationPrincipal User admin,
                                            @PathVariable Long id) {
        materialService.delete(id, admin.getId());   // override: admin can delete any
        return ResponseEntity.ok(wrap(true, Map.of("message", "Material deleted"), null));
    }

    private Map<String, Object> wrap(boolean success, Object data, Object error) {
        return Map.of("success", success, "data", data != null ? data : Map.of(),
                "error", error != null ? error : Map.of(), "timestamp", Instant.now().toString());
    }
}
