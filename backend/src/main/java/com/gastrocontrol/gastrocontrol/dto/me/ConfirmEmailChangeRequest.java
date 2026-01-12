package com.gastrocontrol.gastrocontrol.dto.me;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailChangeRequest(
        @NotBlank String token
) {}
