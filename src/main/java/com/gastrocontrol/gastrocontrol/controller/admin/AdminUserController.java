package com.gastrocontrol.gastrocontrol.controller.admin;


import com.gastrocontrol.gastrocontrol.dto.admin.CreateUserRequest;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.application.service.admin.AdminUserService;
import com.gastrocontrol.gastrocontrol.dto.admin.AdminChangeEmailRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(
            @Valid @RequestBody CreateUserRequest req
    ) {
        adminUserService.createUser(req);
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("User created"));
    }

    @PutMapping("/{id}/email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @PathVariable Long id,
            @Valid @RequestBody AdminChangeEmailRequest req
    ) {
        adminUserService.changeEmail(id, req.newEmail());
        return ResponseEntity.ok(ApiResponse.ok("Email updated"));
    }

    @PostMapping("/{id}/force-password-reset")
    public ResponseEntity<ApiResponse<Void>> forcePasswordReset(@PathVariable Long id) {
        adminUserService.forcePasswordReset(id);
        return ResponseEntity.ok(ApiResponse.ok("Reset email sent"));
    }

    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<ApiResponse<Void>> resendInvite(@PathVariable Long id) {
        adminUserService.resendInvite(id);
        return ResponseEntity.ok(ApiResponse.ok("Invite email sent"));
    }
}
