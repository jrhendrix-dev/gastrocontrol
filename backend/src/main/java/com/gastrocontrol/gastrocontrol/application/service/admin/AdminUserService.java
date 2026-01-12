package com.gastrocontrol.gastrocontrol.application.service.admin;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.admin.CreateUserRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.application.service.mailer.TransactionalEmailService;
import com.gastrocontrol.gastrocontrol.application.service.auth.AccountTokenService;
import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private final AccountTokenService accountTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalEmailService transactionalEmailService;

    @Transactional
    public void createUser(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ValidationException(Map.of("email", "Email already exists"));
        }

        // Temporary password stored hashed; user will set real password via invite link.
        String tempPassword = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(tempPassword);

        UserJpaEntity user = new UserJpaEntity(
                req.email().toLowerCase(),
                hashed,
                req.role(),
                req.active()
        );

        userRepository.save(user);

        var issued = accountTokenService.issue(
                user,
                AccountTokenType.INVITE_SET_PASSWORD,
                Duration.ofHours(24),
                null,
                null
        );

        String url = frontendBaseUrl + "/set-password?token=" + issued.rawToken();

        // ✅ This is the whole point of invite flow
        transactionalEmailService.sendInviteSetPassword(user, url);
    }

    @Transactional
    public void changeEmail(Long userId, String newEmail) {
        String email = newEmail == null ? "" : newEmail.trim().toLowerCase();
        if (email.isBlank()) throw new ValidationException(Map.of("newEmail", "New email is required"));
        if (userRepository.existsByEmail(email)) throw new ValidationException(Map.of("newEmail", "Email already in use"));

        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String oldEmail = user.getEmail();
        user.setEmail(email);

        // revoke sessions
        // if you want, delete refresh tokens here too (requires RefreshTokenRepository injection)
        transactionalEmailService.sendEmailChangedNotification(oldEmail, email);
    }

    @Transactional
    public void forcePasswordReset(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var issued = accountTokenService.issue(
                user,
                AccountTokenType.PASSWORD_RESET,
                Duration.ofMinutes(30),
                null,
                null
        );

        String url = frontendBaseUrl + "/reset-password?token=" + issued.rawToken();
        transactionalEmailService.sendPasswordReset(user, url);
    }

    @Transactional
    public void resendInvite(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var issued = accountTokenService.issue(
                user,
                AccountTokenType.INVITE_SET_PASSWORD,
                Duration.ofHours(24),
                null,
                null
        );

        String url = frontendBaseUrl + "/set-password?token=" + issued.rawToken();
        transactionalEmailService.sendInviteSetPassword(user, url);
    }

}

