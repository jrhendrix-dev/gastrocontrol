package com.gastrocontrol.gastrocontrol.application.service.me;

import com.gastrocontrol.gastrocontrol.application.service.auth.AccountTokenService;
import com.gastrocontrol.gastrocontrol.application.service.mailer.TransactionalEmailService;
import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;
import com.gastrocontrol.gastrocontrol.dto.auth.MeResponse;
import com.gastrocontrol.gastrocontrol.dto.me.ChangePasswordRequest;
import com.gastrocontrol.gastrocontrol.dto.me.ConfirmEmailChangeRequest;
import com.gastrocontrol.gastrocontrol.dto.me.RequestEmailChangeRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.RefreshTokenRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeService {

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountTokenService accountTokenService;
    private final TransactionalEmailService transactionalEmailService;

    @Transactional(Transactional.TxType.SUPPORTS)
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

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest req) {
        if (principal == null) throw new ValidationException(Map.of("auth", "Not authenticated"));
        if (req == null) throw new ValidationException(Map.of("request", "Request body is required"));

        UserJpaEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BusinessRuleViolationException(Map.of("account", "User is disabled"));
        }

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ValidationException(Map.of("currentPassword", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));

        // Best practice: revoke sessions after password change.
        // Minimal version: delete refresh tokens for this user.
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void requestEmailChange(
            UserPrincipal principal,
            RequestEmailChangeRequest req,
            String ip,
            String userAgent
    ) {
        if (principal == null) throw new ValidationException(Map.of("auth", "Not authenticated"));
        if (req == null) throw new ValidationException(Map.of("request", "Request body is required"));

        String newEmail = req.newEmail() == null ? "" : req.newEmail().trim().toLowerCase();
        if (newEmail.isBlank()) throw new ValidationException(Map.of("newEmail", "New email is required"));

        UserJpaEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) throw new BusinessRuleViolationException(Map.of("account", "User is disabled"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ValidationException(Map.of("password", "Password is incorrect"));
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ValidationException(Map.of("newEmail", "New email must be different"));
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new ValidationException(Map.of("newEmail", "Email already in use"));
        }

        var issued = accountTokenService.issueWithNewEmail(
                user,
                AccountTokenType.EMAIL_CHANGE,
                Duration.ofMinutes(30),
                ip,
                userAgent,
                newEmail
        );

        String confirmUrl = frontendBaseUrl + "/confirm-email-change?token=" + issued.rawToken();

        // Send to NEW email address (ownership verification)
        transactionalEmailService.sendEmailChangeConfirmation(
                newEmail,
                displayName(user),
                confirmUrl
        );
    }

    @Transactional
    public void confirmEmailChange(UserPrincipal principal, ConfirmEmailChangeRequest req) {
        if (principal == null) throw new ValidationException(Map.of("auth", "Not authenticated"));
        if (req == null) throw new ValidationException(Map.of("request", "Request body is required"));

        var token = accountTokenService.validateOrThrow(req.token(), AccountTokenType.EMAIL_CHANGE);

        UserJpaEntity tokenUser = token.getUser();
        if (!tokenUser.getId().equals(principal.getId())) {
            throw new ValidationException(Map.of("token", "Token does not belong to this user"));
        }
        if (!tokenUser.isActive()) throw new BusinessRuleViolationException(Map.of("account", "User is disabled"));

        String newEmail = token.getNewEmail();
        if (newEmail == null || newEmail.isBlank()) {
            throw new ValidationException(Map.of("token", "Token missing new email"));
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new ValidationException(Map.of("newEmail", "Email already in use"));
        }

        String oldEmail = tokenUser.getEmail();
        tokenUser.setEmail(newEmail);
        token.markUsed();

        // Security: revoke sessions so user must login again with new identity.
        refreshTokenRepository.deleteByUser(tokenUser);

        // Notify old email (security)
        transactionalEmailService.sendEmailChangedNotification(oldEmail, newEmail);
    }

    private static String displayName(UserJpaEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isBlank() ? "" : full;
    }

    @Transactional
    public MeResponse updateProfile(UserPrincipal principal, com.gastrocontrol.gastrocontrol.dto.me.UpdateProfileRequest req) {
        if (principal == null) throw new ValidationException(Map.of("auth", "Not authenticated"));
        if (req == null) throw new ValidationException(Map.of("request", "Request body is required"));

        UserJpaEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BusinessRuleViolationException(Map.of("account", "User is disabled"));
        }

        if (req.firstName() != null) user.setFirstName(normalize(req.firstName()));
        if (req.lastName() != null) user.setLastName(normalize(req.lastName()));
        if (req.phone() != null) user.setPhone(normalize(req.phone()));

        // Explicit save is fine; JPA dirty checking would also persist at tx commit.
        userRepository.save(user);

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

    /**
     * Normalizes input strings so:
     * - trims whitespace
     * - converts empty/blank to null (interpreted as "clear the field")
     */
    private static String normalize(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
