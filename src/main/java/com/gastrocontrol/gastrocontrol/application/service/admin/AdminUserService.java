package com.gastrocontrol.gastrocontrol.application.service.admin;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.admin.CreateUserRequest;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.application.service.mailer.TransactionalEmailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalEmailService transactionalEmailService;

    @Transactional
    public void createUser(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ValidationException(Map.of("email", "Email already exists"));
        }

        // Optional: random password + reset flow later
        String tempPassword = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(tempPassword);

        UserJpaEntity user = new UserJpaEntity(
                req.email().toLowerCase(),
                hashed,
                req.role(),
                req.active()
        );

        userRepository.save(user);

        transactionalEmailService.sendAdminCreatedAccount(user, req.role());

        // Later:
        // - send invite email
        // - force password reset
    }
}

