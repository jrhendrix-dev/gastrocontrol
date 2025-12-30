package com.gastrocontrol.gastrocontrol.service.auth;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.auth.LoginRequest;
import com.gastrocontrol.gastrocontrol.dto.auth.RegisterRequest;
import com.gastrocontrol.gastrocontrol.dto.auth.RegisterResponse;
import com.gastrocontrol.gastrocontrol.entity.UserJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.enums.UserRole;
import com.gastrocontrol.gastrocontrol.repository.UserRepository;
import com.gastrocontrol.gastrocontrol.security.JwtService;
import com.gastrocontrol.gastrocontrol.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<UserRole> SELF_REGISTER_ALLOWED_ROLES =
            EnumSet.of(UserRole.STAFF, UserRole.MANAGER);
    // If later you want public customers: EnumSet.of(UserRole.CUSTOMER)

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (req == null) {
            throw new ValidationException(Map.of("request", "Request body is required"));
        }

        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        String rawRole = req.role() == null ? "" : req.role().trim().toUpperCase();

        if (email.isBlank()) {
            throw new ValidationException(Map.of("email", "Email is required"));
        }

        if (userRepository.existsByEmail(email)) {
            throw new ValidationException(Map.of("email", "Email already in use"));
        }

        UserRole role = parseRoleOrThrow(rawRole);

        if (!SELF_REGISTER_ALLOWED_ROLES.contains(role)) {
            throw new ValidationException(Map.of(
                    "role",
                    "Role not allowed for self-registration: " + role + ". Allowed: " + SELF_REGISTER_ALLOWED_ROLES
            ));
        }

        String hashed = passwordEncoder.encode(req.password());

        UserJpaEntity user = new UserJpaEntity(email, hashed, role, true);
        UserJpaEntity saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getEmail());
    }

    public String login(LoginRequest req) {
        if (req == null) {
            throw new ValidationException(Map.of("request", "Request body is required"));
        }

        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.isBlank()) {
            throw new ValidationException(Map.of("email", "Email is required"));
        }

        try {
            // Validates password using your UserDetailsService + PasswordEncoder
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.password())
            );
        } catch (AuthenticationException ex) {
            // Don't leak which part failed
            throw new ValidationException(Map.of("credentials", "Invalid email or password"));
        }

        UserJpaEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        return jwtService.generateToken(new UserPrincipal(user));
    }

    private static UserRole parseRoleOrThrow(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new ValidationException(Map.of("role", "Role is required"));
        }

        try {
            return UserRole.valueOf(rawRole);
        } catch (Exception ex) {
            throw new ValidationException(Map.of("role", "Invalid role: " + rawRole));
        }
    }
}
