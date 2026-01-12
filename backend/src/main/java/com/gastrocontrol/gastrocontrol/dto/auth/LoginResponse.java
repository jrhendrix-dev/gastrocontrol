package com.gastrocontrol.gastrocontrol.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
