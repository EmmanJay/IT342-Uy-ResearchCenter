package edu.cit.uy.researchcenter.features.repository;

import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final RepositoryService repositoryService;
    private final UserService userService;

    @GetMapping("/{token}")
    public ResponseEntity<?> getInvitation(@AuthenticationPrincipal UserDetails principal, @PathVariable String token) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, repositoryService.getInvitation(token, user.getId()), null));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<?> acceptInvitation(@AuthenticationPrincipal UserDetails principal, @PathVariable String token) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, repositoryService.acceptInvitation(token, user.getId()), null));
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
