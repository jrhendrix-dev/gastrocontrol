// src/main/java/com/gastrocontrol/gastrocontrol/application/service/admin/AdminUserService.java
package com.gastrocontrol.gastrocontrol.application.service.admin;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.AccountTokenType;
import com.gastrocontrol.gastrocontrol.domain.enums.UserRole;
import com.gastrocontrol.gastrocontrol.dto.admin.CreateUserRequest;
import com.gastrocontrol.gastrocontrol.dto.admin.UserResponse;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.application.service.mailer.TransactionalEmailService;
import com.gastrocontrol.gastrocontrol.application.service.auth.AccountTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for admin-facing user management.
 *
 * <p>Handles account creation, listing, deactivation, email changes,
 * and credential reset flows.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private final AccountTokenService accountTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalEmailService transactionalEmailService;

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated, optionally filtered list of user accounts.
     *
     * @param role   optional role filter; null means all roles
     * @param active optional active-status filter; null means all
     * @param q      optional case-insensitive email partial-match search
     * @param page   zero-based page index
     * @param size   page size
     * @param sort   sort expression e.g. {@code "email,asc"}
     * @return paginated list of user responses, newest first by default
     */
    @Transactional
    public PagedResponse<UserResponse> list(
            UserRole role,
            Boolean active,
            String q,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);

        Specification<UserJpaEntity> spec = Specification.where(null);
        if (role != null)   spec = spec.and(UserSpecifications.hasRole(role));
        if (active != null) spec = spec.and(UserSpecifications.isActive(active));
        if (q != null && !q.isBlank()) spec = spec.and(UserSpecifications.emailContains(q));

        Page<UserResponse> resultPage = userRepository
                .findAll(spec, pageable)
                .map(AdminUserService::toResponse);

        return PagedResponse.from(resultPage);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Creates a new staff/manager/admin account and sends an invite email
     * with a set-password link.
     *
     * @param req the creation request (email, role, active flag)
     * @throws ValidationException if the email is already registered
     */
    @Transactional
    public void createUser(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ValidationException(Map.of("email", "Email already exists"));
        }

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
        transactionalEmailService.sendInviteSetPassword(user, url);
    }

    /**
     * Deactivates a user account, preventing them from logging in.
     *
     * <p>Admins cannot deactivate their own account to prevent self-lockout.</p>
     *
     * @param userId the id of the user to deactivate
     * @throws NotFoundException          if no user with the given id exists
     * @throws BusinessRuleViolationException if the admin tries to deactivate themselves
     * @throws ValidationException        if the account is already inactive
     */
    @Transactional
    public void deactivate(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        String callerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equalsIgnoreCase(callerEmail)) {
            throw new BusinessRuleViolationException(Map.of(
                    "userId", "Admins cannot deactivate their own account"
            ));
        }

        if (!user.isActive()) {
            throw new ValidationException(Map.of("userId", "User is already inactive"));
        }

        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Reactivates a previously deactivated user account.
     *
     * @param userId the id of the user to reactivate
     * @throws NotFoundException   if no user with the given id exists
     * @throws ValidationException if the account is already active
     */
    @Transactional
    public void reactivate(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (user.isActive()) {
            throw new ValidationException(Map.of("userId", "User is already active"));
        }

        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Changes a user's email address (admin override, no confirmation required).
     *
     * @param userId   the user to update
     * @param newEmail the new email address
     * @throws NotFoundException   if the user does not exist
     * @throws ValidationException if the new email is blank or already taken
     */
    @Transactional
    public void changeEmail(Long userId, String newEmail) {
        String email = newEmail == null ? "" : newEmail.trim().toLowerCase();
        if (email.isBlank()) throw new ValidationException(Map.of("email", "Email is required"));
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException(Map.of("email", "Email already in use"));
        }

        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setEmail(email);
        userRepository.save(user);
    }

    /**
     * Sends a password-reset email to the specified user.
     *
     * @param userId the user to send the reset link to
     * @throws NotFoundException if the user does not exist
     */
    @Transactional
    public void forcePasswordReset(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        var issued = accountTokenService.issue(
                user,
                AccountTokenType.PASSWORD_RESET,
                Duration.ofHours(2),
                null,
                null
        );

        String url = frontendBaseUrl + "/reset-password?token=" + issued.rawToken();
        transactionalEmailService.sendPasswordReset(user, url);
    }

    /**
     * Resends the set-password invite email to a user who has not yet accepted it.
     *
     * @param userId the user to re-invite
     * @throws NotFoundException if the user does not exist
     */
    @Transactional
    public void resendInvite(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

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

    // ── Private helpers ───────────────────────────────────────────────────────

    private static UserResponse toResponse(UserJpaEntity u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhone(),
                u.getRole(),
                u.isActive(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }

    private static Pageable toPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String field = parts.length > 0 ? parts[0].trim() : "createdAt";
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }
}