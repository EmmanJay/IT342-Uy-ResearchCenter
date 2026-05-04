package edu.cit.uy.researchcenter.features.request;

import edu.cit.uy.researchcenter.features.request.dto.*;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import edu.cit.uy.researchcenter.features.request.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails principal,
                                    @Valid @RequestBody CreateRequestDto dto) {
        User user = userService.findByEmail(principal.getUsername());
        RequestResponse req = requestService.create(user, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(wrap(true, req, null));
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, requestService.getAll(user.getId()), null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, requestService.getById(id, user.getId()), null));
    }

    @PostMapping("/{id}/fulfill")
    public ResponseEntity<?> fulfill(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody FulfillRequestDto dto) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, requestService.fulfill(id, user.getId(), dto), null));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@AuthenticationPrincipal UserDetails principal,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, requestService.close(id, user.getId(), body.get("note")), null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails principal,
                                    @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        requestService.delete(id, user.getId());
        return ResponseEntity.ok(wrap(true, null, null));
    }

    @PutMapping("/{id}/material")
    public ResponseEntity<?> updateMaterial(@AuthenticationPrincipal UserDetails principal,
                                           @PathVariable Long id,
                                           @Valid @RequestBody FulfillRequestDto dto) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, requestService.updateMaterial(id, user.getId(), dto), null));
    }

    private Map<String, Object> wrap(boolean success, Object data, Object error) {
        return Map.of("success", success, "data", data != null ? data : Map.of(),
                "error", error != null ? error : Map.of(), "timestamp", Instant.now().toString());
    }
}
