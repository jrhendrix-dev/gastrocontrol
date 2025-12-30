package com.gastrocontrol.gastrocontrol.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String role // "STAFF" or "MANAGER"
) {}
