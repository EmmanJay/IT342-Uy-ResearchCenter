package edu.cit.uy.researchcenter.features.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import edu.cit.uy.researchcenter.features.auth.dto.AuthResponse;
import edu.cit.uy.researchcenter.features.auth.model.RefreshToken;
import edu.cit.uy.researchcenter.features.auth.model.Role;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.auth.repository.RefreshTokenRepository;
import edu.cit.uy.researchcenter.features.auth.repository.RoleRepository;
import edu.cit.uy.researchcenter.features.auth.repository.UserRepository;
import edu.cit.uy.researchcenter.features.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserService userService;

    @Value("${google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticateWithGoogle(String idToken) {
        // 1. Verify the ID token with Google
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken googleIdToken;
        try {
            googleIdToken = verifier.verify(idToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid Google token");
        }

        if (googleIdToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google token verification failed");
        }

        // 2. Extract user info from verified token
        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        String firstname = (String) payload.get("given_name");
        String lastname = (String) payload.get("family_name");
        String googleId = payload.getSubject();

        if (firstname == null) firstname = email.split("@")[0];
        if (lastname == null) lastname = "";

        // 3. Find existing user or create new one
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update googleId if not set
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
            }
        } else {
            // New user — create account
            Role userRole = roleRepository.findByName("RESEARCHER")
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found"));

            user = User.builder()
                    .email(email)
                    .firstName(firstname)
                    .lastName(lastname)
                    .googleId(googleId)
                    .password("") // no password for Google-only users
                    .role(userRole)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            userRepository.save(user);
        }

        // 4. Generate JWT tokens — same as email/password login
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        
        // Invalidate old tokens and create new refresh token
        refreshTokenRepository.invalidateAllByUser(user);
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .role(user.getRole().getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpiration()))
                .isValid(true)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);
        return token;
    }
}
