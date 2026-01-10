package com.gastrocontrol.gastrocontrol.application.service.auth;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.AccountTokenJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.AccountTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Issues and validates one-time account tokens (invite + password reset).
 */
@Service
@RequiredArgsConstructor
public class AccountTokenService {

    private final AccountTokenRepository accountTokenRepository;

    @Transactional
    public IssuedToken issue(UserJpaEntity user, AccountTokenType type, Duration ttl, String ip, String userAgent) {
        if (user == null) throw new ValidationException(Map.of("user", "User is required"));
        if (type == null) throw new ValidationException(Map.of("type", "Token type is required"));
        if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new ValidationException(Map.of("ttl", "TTL must be > 0"));

        // Invalidate previous active tokens of same type (optional hardening)
        for (AccountTokenJpaEntity t : accountTokenRepository.findAllByUserAndTypeAndUsedAtIsNull(user, type)) {
            t.markUsed();
        }

        String raw = generateSecureToken();
        String hash = sha256Base64(raw);
        Instant exp = Instant.now().plus(ttl);

        accountTokenRepository.save(new AccountTokenJpaEntity(user, type, hash, exp, ip, userAgent));

        return new IssuedToken(raw, exp);
    }

    @Transactional
    public AccountTokenJpaEntity validateOrThrow(String rawToken, AccountTokenType expectedType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ValidationException(Map.of("token", "Token is required"));
        }
        if (expectedType == null) {
            throw new ValidationException(Map.of("type", "Token type is required"));
        }

        String hash = sha256Base64(rawToken);
        AccountTokenJpaEntity token = accountTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ValidationException(Map.of("token", "Invalid token")));

        if (token.getType() != expectedType) {
            throw new ValidationException(Map.of("token", "Invalid token type"));
        }
        if (token.isUsed()) {
            throw new ValidationException(Map.of("token", "Token already used"));
        }
        if (token.isExpired()) {
            throw new ValidationException(Map.of("token", "Token expired"));
        }

        return token;
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

    public record IssuedToken(String rawToken, Instant expiresAt) {}
}
