package edu.cit.uy.researchcenter.features.repository;

import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.user.UserService;
import edu.cit.uy.researchcenter.features.repository.model.PrivateNote;
import edu.cit.uy.researchcenter.features.repository.model.RepositoryUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}")
@RequiredArgsConstructor
public class RepositoryExtraController {

    private final RepositoryExtraService extraService;
    private final UserService userService;

    private User getAuthenticatedUser(UserDetails principal) {
        return userService.findByEmail(principal.getUsername());
    }

    private Map<String, Object> wrap(boolean success, Object data, String message) {
        return Map.of("success", success, "data", data != null ? data : "", "message", message != null ? message : "");
    }

    @PostMapping("/bookmark")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long repositoryId, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        boolean isBookmarked = extraService.toggleBookmark(user.getId(), repositoryId);
        return ResponseEntity.ok(wrap(true, Map.of("bookmarked", isBookmarked), null));
    }

    @GetMapping("/bookmark")
    public ResponseEntity<?> getBookmarkStatus(@PathVariable Long repositoryId, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        boolean isBookmarked = extraService.isBookmarked(user.getId(), repositoryId);
        return ResponseEntity.ok(wrap(true, Map.of("bookmarked", isBookmarked), null));
    }

    @GetMapping("/note")
    public ResponseEntity<?> getPrivateNote(@PathVariable Long repositoryId, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        PrivateNote note = extraService.getPrivateNote(user.getId(), repositoryId);
        return ResponseEntity.ok(wrap(true, note, null));
    }

    @PutMapping("/note")
    public ResponseEntity<?> savePrivateNote(@PathVariable Long repositoryId, @RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        PrivateNote note = extraService.savePrivateNote(user.getId(), repositoryId, body.get("content"));
        return ResponseEntity.ok(wrap(true, note, null));
    }

    @GetMapping("/updates")
    public ResponseEntity<?> getUpdates(@PathVariable Long repositoryId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<RepositoryUpdate> updates = extraService.getUpdates(repositoryId, page, size);
        return ResponseEntity.ok(wrap(true, updates.getContent(), null));
    }

    @PostMapping("/updates")
    public ResponseEntity<?> addUpdate(@PathVariable Long repositoryId, @RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        RepositoryUpdate update = extraService.addUpdate(repositoryId, user.getId(), user.getFirstName() + " " + user.getLastName(), body.get("content"));
        return ResponseEntity.ok(wrap(true, update, null));
    }

    @PutMapping("/updates/{updateId}")
    public ResponseEntity<?> editUpdate(@PathVariable Long repositoryId, @PathVariable Long updateId, @RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        RepositoryUpdate update = extraService.editUpdate(updateId, user.getId(), body.get("content"));
        return ResponseEntity.ok(wrap(true, update, null));
    }

    @DeleteMapping("/updates/{updateId}")
    public ResponseEntity<?> deleteUpdate(@PathVariable Long repositoryId, @PathVariable Long updateId, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        extraService.deleteUpdate(updateId, user.getId(), repositoryId);
        return ResponseEntity.ok(wrap(true, Map.of("message", "Update deleted"), null));
    }
}
