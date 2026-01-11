package com.gastrocontrol.gastrocontrol.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminChangeEmailRequest(
        @Email @NotBlank String newEmail
) {}
