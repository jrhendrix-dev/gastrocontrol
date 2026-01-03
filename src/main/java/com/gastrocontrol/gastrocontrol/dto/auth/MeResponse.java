package com.gastrocontrol.gastrocontrol.dto.auth;

import com.gastrocontrol.gastrocontrol.entity.enums.UserRole;

import java.time.Instant;

public record MeResponse(
        Long id,
        String email,
        UserRole role,
        boolean active,
        String firstName,
        String lastName,
        String phone,
        Instant createdAt,
        Instant lastLoginAt
) {}
