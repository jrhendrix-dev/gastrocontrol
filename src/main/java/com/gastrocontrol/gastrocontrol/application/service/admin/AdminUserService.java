package com.gastrocontrol.gastrocontrol.application.service.admin;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.admin.CreateUserRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.application.service.mailer.TransactionalEmailService;
import com.gastrocontrol.gastrocontrol.application.service.auth.AccountTokenService;
import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;


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

}

