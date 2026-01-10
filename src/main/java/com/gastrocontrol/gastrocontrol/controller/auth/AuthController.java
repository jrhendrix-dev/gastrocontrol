package com.gastrocontrol.gastrocontrol.controller.auth;

import com.gastrocontrol.gastrocontrol.dto.auth.*;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.security.UserPrincipal;
import com.gastrocontrol.gastrocontrol.application.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${security.refresh.cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${security.refresh.cookie.same-site:Lax}")
    private String refreshSameSite;

    @Value("${security.refresh.cookie.secure:false}")
    private boolean refreshSecure;

    @Value("${security.refresh.expiration-days:14}")
    private long refreshExpirationDays;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest req) {
        RegisterResponse created = authService.register(req);
        return ResponseEntity.status(201).body(
                ApiResponse.ok("User " + created.getEmail() + " successfully registered", created)
        );
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authService.login(req, clientIp(request), request.getHeader("User-Agent"));

        setRefreshCookie(response, result.refreshTokenRaw());

        return ResponseEntity.ok(ApiResponse.ok("Login successful", result.body()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = "${security.refresh.cookie.name:refresh_token}", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authService.refresh(refreshToken, clientIp(request), request.getHeader("User-Agent"));

        // rotate cookie
        setRefreshCookie(response, result.refreshTokenRaw());

        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", result.body()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "${security.refresh.cookie.name:refresh_token}", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        MeResponse body = authService.me(principal);
        return ResponseEntity.ok(ApiResponse.ok("Me", body));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req,
            HttpServletRequest request
    ) {
        authService.forgotPassword(req, clientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link was sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Password updated"));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(@Valid @RequestBody SetPasswordRequest req) {
        authService.acceptInvite(req);
        return ResponseEntity.ok(ApiResponse.ok("Password set"));
    }


    private void setRefreshCookie(HttpServletResponse response, String refreshTokenRaw) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshTokenRaw)
                .httpOnly(true)
                .secure(refreshSecure)
                .path("/api/auth/refresh") // tight scope
                .maxAge(Duration.ofDays(refreshExpirationDays))
                .sameSite(refreshSameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshSecure)
                .path("/api/auth/refresh")
                .maxAge(Duration.ZERO)
                .sameSite(refreshSameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private static String clientIp(HttpServletRequest request) {
        // If you later add a reverse proxy, you can use X-Forwarded-For here.
        return request.getRemoteAddr();
    }


}
