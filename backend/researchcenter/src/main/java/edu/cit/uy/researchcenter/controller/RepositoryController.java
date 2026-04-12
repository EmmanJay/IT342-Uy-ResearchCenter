package edu.cit.uy.researchcenter.controller;

import edu.cit.uy.researchcenter.dto.*;
import edu.cit.uy.researchcenter.model.User;
import edu.cit.uy.researchcenter.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import edu.cit.uy.researchcenter.service.RepositoryService;
import edu.cit.uy.researchcenter.service.MaterialService;
import edu.cit.uy.researchcenter.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final MaterialService materialService;
    private final RequestService requestService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails principal,
                                    @Valid @RequestBody CreateRepositoryRequest req) {
        User user = userService.findByEmail(principal.getUsername());
        RepositoryResponse repo = repositoryService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wrap(true, repo, null));
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername());
        List<RepositoryResponse> repos = repositoryService.getAllForUser(user.getId());
        return ResponseEntity.ok(wrap(true, repos, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        RepositoryResponse repo = repositoryService.getById(id, user.getId());
        return ResponseEntity.ok(wrap(true, repo, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody CreateRepositoryRequest req) {
        User user = userService.findByEmail(principal.getUsername());
        RepositoryResponse repo = repositoryService.update(id, user.getId(), req);
        return ResponseEntity.ok(wrap(true, repo, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails principal,
                                    @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        repositoryService.delete(id, user.getId());
        return ResponseEntity.ok(wrap(true, Map.of("message", "Repository deleted successfully"), null));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> inviteMember(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        User user = userService.findByEmail(principal.getUsername());
        repositoryService.inviteMember(id, user.getId(), body.get("email"));
        return ResponseEntity.ok(wrap(true, Map.of("message", "Invitation sent successfully"), null));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(@AuthenticationPrincipal UserDetails principal,
                                        @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        List<RepositoryResponse.MemberDto> members = repositoryService.getMembers(id, user.getId());
        return ResponseEntity.ok(wrap(true, members, null));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id,
                                          @PathVariable Long userId) {
        User user = userService.findByEmail(principal.getUsername());
        repositoryService.removeMember(id, user.getId(), userId);
        return ResponseEntity.ok(wrap(true, Map.of("message", "Member removed successfully"), null));
    }

    // ── Materials by repo ─────────────────────────────────────────────────
    @GetMapping("/{id}/materials")
    public ResponseEntity<?> getMaterials(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, materialService.getByRepo(id, user.getId()), null));
    }

    // ── Requests by repo ──────────────────────────────────────────────────
    @GetMapping("/{id}/requests")
    public ResponseEntity<?> getRequests(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, requestService.getByRepo(id, user.getId()), null));
    }

    private Map<String, Object> wrap(boolean success, Object data, Object error) {
        return Map.of(
                "success", success,
                "data", data != null ? data : Map.of(),
                "error", error != null ? error : Map.of(),
                "timestamp", Instant.now().toString()
        );
    }
}
