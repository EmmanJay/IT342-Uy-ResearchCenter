package edu.cit.uy.researchcenter.features.auth;

import edu.cit.uy.researchcenter.features.auth.dto.AuthResponse;
import edu.cit.uy.researchcenter.features.auth.dto.LoginRequest;
import edu.cit.uy.researchcenter.features.auth.dto.RegisterRequest;
import edu.cit.uy.researchcenter.features.auth.model.RefreshToken;
import edu.cit.uy.researchcenter.features.auth.model.Role;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.auth.repository.RefreshTokenRepository;
import edu.cit.uy.researchcenter.features.auth.repository.RoleRepository;
import edu.cit.uy.researchcenter.features.auth.repository.UserRepository;
import edu.cit.uy.researchcenter.features.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        Role defaultRole = roleRepository.findByName("RESEARCHER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name("RESEARCHER").description("Default researcher role").build()
                ));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .role(defaultRole)
                .build();

        user = userRepository.save(user);

        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.LockedException e) {
            throw new InvalidCredentialsException("Account is currently suspended");
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userService.findByEmail(request.getEmail());
        if (Boolean.TRUE.equals(user.getSuspended())) {
            throw new InvalidCredentialsException("Your account is suspended. Please contact administrator");
        }
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);

        refreshTokenRepository.invalidateAllByUser(user);
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpiration()))
                .isValid(true)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .profilePicture(user.getProfilePicture())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // Custom exceptions
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
