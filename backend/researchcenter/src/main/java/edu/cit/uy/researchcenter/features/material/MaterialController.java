package edu.cit.uy.researchcenter.controller;

import edu.cit.uy.researchcenter.dto.*;
import edu.cit.uy.researchcenter.model.User;
import edu.cit.uy.researchcenter.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import edu.cit.uy.researchcenter.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private static final Logger logger = LoggerFactory.getLogger(MaterialController.class);
    private final MaterialService materialService;
    private final UserService userService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id,
                                          @Valid @RequestBody UpdateMaterialStatusRequest req) {
        try {
            User user = userService.findByEmail(principal.getUsername());
            String status = materialService.updateUserStatus(id, user.getId(), req.getStatus());
            return ResponseEntity.ok(wrap(true, Map.of("status", status), null));
        } catch (Exception e) {
            // Temporary debugging: include exception message in response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrap(false, null, Map.of("message", e.getMessage())));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails principal,
                                    @Valid @RequestBody CreateMaterialRequest req) {
        User user = userService.findByEmail(principal.getUsername());
        MaterialResponse mat = materialService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(wrap(true, mat, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails principal,
                                    @PathVariable Long id,
                                    @RequestBody CreateMaterialRequest req) {
        User user = userService.findByEmail(principal.getUsername());
        MaterialResponse mat = materialService.update(id, user.getId(), req);
        return ResponseEntity.ok(wrap(true, mat, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id) {
        User user = userService.findByEmail(principal.getUsername());
        return ResponseEntity.ok(wrap(true, materialService.getById(id, user.getId()), null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails principal,
                                    @PathVariable Long id) {
        try {
            User user = userService.findByEmail(principal.getUsername());
            logger.info("Deleting material id=" + id + " by user id=" + user.getId());
            materialService.delete(id, user.getId());
            return ResponseEntity.ok(wrap(true, Map.of("message", "Material deleted successfully"), null));
        } catch (Exception e) {
            logger.error("Error deleting material id=" + id, e);
            throw e;  // Let GlobalExceptionHandler handle it
        }
    }

    private Map<String, Object> wrap(boolean success, Object data, Object error) {
        return Map.of("success", success, "data", data != null ? data : Map.of(),
                "error", error != null ? error : Map.of(), "timestamp", Instant.now().toString());
    }
}
