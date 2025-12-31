package com.gastrocontrol.gastrocontrol.dto.auth;

public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
