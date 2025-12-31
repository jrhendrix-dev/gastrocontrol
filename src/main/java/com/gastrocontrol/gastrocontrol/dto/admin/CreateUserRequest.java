package com.gastrocontrol.gastrocontrol.dto.admin;

import com.gastrocontrol.gastrocontrol.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @Email String email,
        @NotNull UserRole role,
        boolean active
) {}
