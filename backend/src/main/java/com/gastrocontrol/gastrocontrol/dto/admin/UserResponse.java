// src/main/java/com/gastrocontrol/gastrocontrol/dto/admin/UserResponse.java
package com.gastrocontrol.gastrocontrol.dto.admin;

import com.gastrocontrol.gastrocontrol.domain.enums.UserRole;

import java.time.Instant;

/**
 * Admin-facing representation of a user account.
 *
 * <p>Mirrors the fields available in {@code UserJpaEntity} but omits the
 * password hash. Used for the admin user management list and detail views.</p>
 *
 * @param id          the user's primary key
 * @param email       the login email address
 * @param firstName   optional first name
 * @param lastName    optional last name
 * @param phone       optional phone number
 * @param role        the user's assigned role
 * @param active      whether the account is currently enabled
 * @param createdAt   when the account was created
 * @param lastLoginAt when the user last authenticated; null if never logged in
 */
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt
) {}