package com.gastrocontrol.gastrocontrol.controller.me;

import com.gastrocontrol.gastrocontrol.application.service.me.MeService;
import com.gastrocontrol.gastrocontrol.dto.auth.MeResponse;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.me.ChangePasswordRequest;
import com.gastrocontrol.gastrocontrol.dto.me.ConfirmEmailChangeRequest;
import com.gastrocontrol.gastrocontrol.dto.me.RequestEmailChangeRequest;
import com.gastrocontrol.gastrocontrol.dto.me.UpdateProfileRequest;
import com.gastrocontrol.gastrocontrol.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Me", meService.me(principal)));
    }


    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        meService.changePassword(principal, req);
        return ResponseEntity.ok(ApiResponse.ok("Password updated"));
    }

    @PostMapping("/email-change/request")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RequestEmailChangeRequest req,
            HttpServletRequest request
    ) {
        meService.requestEmailChange(principal, req, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.ok("Confirmation sent to new email"));
    }

    @PostMapping("/email-change/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConfirmEmailChangeRequest req
    ) {
        meService.confirmEmailChange(principal, req);
        return ResponseEntity.ok(ApiResponse.ok("Email updated"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<MeResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", meService.updateProfile(principal, req)));
    }
}
