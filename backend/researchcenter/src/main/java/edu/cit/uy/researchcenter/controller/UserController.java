package edu.cit.uy.researchcenter.controller;

import edu.cit.uy.researchcenter.dto.ApiResponse;
import edu.cit.uy.researchcenter.dto.AuthResponse;
import edu.cit.uy.researchcenter.model.User;
import edu.cit.uy.researchcenter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import edu.cit.uy.researchcenter.repository.UserRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AuthResponse>> searchUser(@RequestParam(required = true) String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(null));
            }
            
            Optional<User> userOpt = userRepository.findByEmailContainingIgnoreCase(email.trim());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                AuthResponse response = AuthResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstname(user.getFirstName() != null ? user.getFirstName() : "")
                    .lastname(user.getLastName() != null ? user.getLastName() : "")
                    .build();
                return ResponseEntity.ok(ApiResponse.success(response));
            }
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());

        AuthResponse response = AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
