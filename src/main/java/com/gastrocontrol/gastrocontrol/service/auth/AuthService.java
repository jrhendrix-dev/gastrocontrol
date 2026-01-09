package com.gastrocontrol.gastrocontrol.service.auth;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.auth.*;
import com.gastrocontrol.gastrocontrol.entity.RefreshTokenJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.enums.UserRole;
import com.gastrocontrol.gastrocontrol.repository.RefreshTokenRepository;
import com.gastrocontrol.gastrocontrol.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.security.JwtService;
import com.gastrocontrol.gastrocontrol.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<UserRole> SELF_REGISTER_ALLOWED_ROLES =
            EnumSet.of(UserRole.CUSTOMER);


    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${security.jwt.expiration-seconds:900}")
    private long accessExpirationSeconds;

    @Value("${security.refresh.expiration-days:14}")
    private long refreshExpirationDays;

    // ---------------- Register (public self-register) ----------------

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (req == null) throw new ValidationException(Map.of("request", "Request body is required"));

        String email = req.email() == null ? "" : req.email().trim().toLowerCase();

        if (email.isBlank()) throw new ValidationException(Map.of("email", "Email is required"));
        if (userRepository.existsByEmail(email)) throw new ValidationException(Map.of("email", "Email already in use"));

        UserRole role = UserRole.CUSTOMER;

        if (!SELF_REGISTER_ALLOWED_ROLES.contains(role)) {
            throw new ValidationException(Map.of(
                    "role",
                    "Role not allowed for self-registration: " + role + ". Allowed: " + SELF_REGISTER_ALLOWED_ROLES
            ));
        }

        String hashed = passwordEncoder.encode(req.password());
        UserJpaEntity saved = userRepository.save(new UserJpaEntity(email, hashed, role, true));



        return new RegisterResponse(saved.getId(), saved.getEmail());
    }

    // ---------------- Login ----------------

    @Transactional
    public AuthLoginResult login(LoginRequest req, String ip, String userAgent) {
        if (req == null) throw new ValidationException(Map.of("request", "Request body is required"));

        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.isBlank()) throw new ValidationException(Map.of("email", "Email is required"));

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        } catch (AuthenticationException ex) {
            throw new ValidationException(Map.of("credentials", "Invalid email or password"));
        }

        UserJpaEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        if (!user.isActive()) {
            throw new BusinessRuleViolationException(Map.of("account", "User is disabled"));
        }

        user.setLastLoginAt(Instant.now());

        String accessToken = jwtService.generateToken(new UserPrincipal(user));

        // Create refresh token (random string), store only hash
        String refreshRaw = generateSecureToken();
        String refreshHash = sha256Base64(refreshRaw);

        Instant refreshExp = Instant.now().plusSeconds(refreshExpirationDays * 24L * 3600L);

        refreshTokenRepository.save(new RefreshTokenJpaEntity(
                user,
                refreshHash,
                refreshExp,
                ip,
                userAgent
        ));



        return new AuthLoginResult(
                new LoginResponse(accessToken, "Bearer", accessExpirationSeconds),
                refreshRaw
        );
    }

    // ---------------- Refresh (rotation) ----------------

    @Transactional
    public AuthRefreshResult refresh(String refreshTokenRaw, String ip, String userAgent) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new ValidationException(Map.of("refreshToken", "Refresh token is required"));
        }

        String hash = sha256Base64(refreshTokenRaw);

        RefreshTokenJpaEntity existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ValidationException(Map.of("refreshToken", "Invalid refresh token")));

        if (existing.isRevoked()) {
            throw new ValidationException(Map.of("refreshToken", "Refresh token revoked"));
        }
        if (existing.isExpired()) {
            throw new ValidationException(Map.of("refreshToken", "Refresh token expired"));
        }

        UserJpaEntity user = existing.getUser();
        if (!user.isActive()) {
            throw new BusinessRuleViolationException(Map.of("account", "User is disabled"));
        }

        // Rotate: create a new refresh token, revoke old one linking to new
        String newRefreshRaw = generateSecureToken();
        String newRefreshHash = sha256Base64(newRefreshRaw);
        Instant newExp = Instant.now().plusSeconds(refreshExpirationDays * 24L * 3600L);

        RefreshTokenJpaEntity newToken = refreshTokenRepository.save(new RefreshTokenJpaEntity(
                user, newRefreshHash, newExp, ip, userAgent
        ));

        existing.revoke(Instant.now(), newToken);
        refreshTokenRepository.save(existing);

        String newAccessToken = jwtService.generateToken(new UserPrincipal(user));

        return new AuthRefreshResult(
                new RefreshResponse(newAccessToken, "Bearer", accessExpirationSeconds),
                newRefreshRaw
        );
    }

    // ---------------- Logout ----------------

    @Transactional
    public void logout(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) return;

        String hash = sha256Base64(refreshTokenRaw);

        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            if (!rt.isRevoked()) {
                rt.revoke(Instant.now(), null);
                refreshTokenRepository.save(rt);
            }
        });
    }

    // ---------------- Me --------------------
    @Transactional
    public MeResponse me(UserPrincipal principal) {
        if (principal == null) {
            throw new ValidationException(Map.of("auth", "Not authenticated"));
        }

        UserJpaEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }


    // ---------------- Helpers ----------------

    private static UserRole parseRoleOrThrow(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) throw new ValidationException(Map.of("role", "Role is required"));
        try {
            return UserRole.valueOf(rawRole);
        } catch (Exception ex) {
            throw new ValidationException(Map.of("role", "Invalid role: " + rawRole));
        }
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Base64(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash token", e);
        }
    }

    // Small records for service internal return (access response + refresh raw)
    public record AuthLoginResult(LoginResponse body, String refreshTokenRaw) {}
    public record AuthRefreshResult(RefreshResponse body, String refreshTokenRaw) {}
}
