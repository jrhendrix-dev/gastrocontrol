package com.gastrocontrol.gastrocontrol.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class RefreshTokenSupport {
    private static final SecureRandom RNG = new SecureRandom();

    private RefreshTokenSupport() {}

    public static String newRawToken() {
        byte[] bytes = new byte[64];
        RNG.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes); // 128 hex chars
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest); // 64 hex chars
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }
}
