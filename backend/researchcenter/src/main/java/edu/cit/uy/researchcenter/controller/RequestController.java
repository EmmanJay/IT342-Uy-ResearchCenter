package edu.cit.uy.researchcenter.controller;

import edu.cit.uy.researchcenter.dto.*;
import edu.cit.uy.researchcenter.model.User;
import edu.cit.uy.researchcenter.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import edu.cit.uy.researchcenter.service.RequestService;
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
