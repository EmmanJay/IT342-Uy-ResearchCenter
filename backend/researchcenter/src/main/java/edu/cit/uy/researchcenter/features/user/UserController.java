package edu.cit.uy.researchcenter.features.user;

import edu.cit.uy.researchcenter.shared.response.ApiResponse;
import edu.cit.uy.researchcenter.features.auth.dto.AuthResponse;
import edu.cit.uy.researchcenter.features.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import edu.cit.uy.researchcenter.features.auth.repository.UserRepository;

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
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @org.springframework.web.bind.annotation.PutMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> updateCurrentUser(Authentication authentication, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> updates) {
        User user = userService.findByEmail(authentication.getName());
        if (updates.containsKey("firstname")) user.setFirstName(updates.get("firstname"));
        if (updates.containsKey("lastname")) user.setLastName(updates.get("lastname"));
        if (updates.containsKey("profilePicture")) user.setProfilePicture(updates.get("profilePicture"));
        
        userRepository.save(user);

        AuthResponse response = AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
