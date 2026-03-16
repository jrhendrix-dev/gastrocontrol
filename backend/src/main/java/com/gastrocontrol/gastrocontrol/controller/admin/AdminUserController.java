// src/main/java/com/gastrocontrol/gastrocontrol/controller/admin/AdminUserController.java
package com.gastrocontrol.gastrocontrol.controller.admin;

import com.gastrocontrol.gastrocontrol.domain.enums.UserRole;
import com.gastrocontrol.gastrocontrol.dto.admin.AdminChangeEmailRequest;
import com.gastrocontrol.gastrocontrol.dto.admin.CreateUserRequest;
import com.gastrocontrol.gastrocontrol.dto.admin.UserResponse;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.application.service.admin.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoints for user account management.
 *
 * <p>All endpoints require {@code ROLE_ADMIN} (enforced via {@code @PreAuthorize}
 * in addition to the path-level rule in {@code SecurityConfig}).</p>
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Lists all user accounts with optional role, active-status, and email filters.
     *
     * @param role   optional role filter (STAFF, MANAGER, ADMIN, CUSTOMER)
     * @param active optional active-status filter
     * @param q      optional case-insensitive email partial-match
     * @param page   zero-based page index (default 0)
     * @param size   page size (default 20)
     * @param sort   sort expression e.g. {@code "email,asc"} (default {@code "createdAt,desc"})
     * @return paginated list of user responses
     */
    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(adminUserService.list(role, active, q, page, size, sort));
    }

    /**
     * Creates a new user account and sends an invite email with a set-password link.
     *
     * @param req the creation request (email, role, active flag)
     * @return 201 Created with success message
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(
            @Valid @RequestBody CreateUserRequest req
    ) {
        adminUserService.createUser(req);
        return ResponseEntity.status(201).body(ApiResponse.ok("User created"));
    }

    /**
     * Deactivates a user account. The user will not be able to log in.
     *
     * <p>Admins cannot deactivate their own account.</p>
     *
     * @param id the user to deactivate
     * @return 200 OK with success message
     */
    @PostMapping("/{id}/actions/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        adminUserService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated"));
    }

    /**
     * Reactivates a previously deactivated user account.
     *
     * @param id the user to reactivate
     * @return 200 OK with success message
     */
    @PostMapping("/{id}/actions/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivate(@PathVariable Long id) {
        adminUserService.reactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("User reactivated"));
    }

    /**
     * Changes a user's email address (admin override, no confirmation flow required).
     *
     * @param id  the user to update
     * @param req the new email address
     * @return 200 OK with success message
     */
    @PutMapping("/{id}/email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @PathVariable Long id,
            @Valid @RequestBody AdminChangeEmailRequest req
    ) {
        adminUserService.changeEmail(id, req.newEmail());
        return ResponseEntity.ok(ApiResponse.ok("Email updated"));
    }

    /**
     * Sends a password-reset email to the specified user.
     *
     * @param id the user to send the reset link to
     * @return 200 OK with success message
     */
    @PostMapping("/{id}/force-password-reset")
    public ResponseEntity<ApiResponse<Void>> forcePasswordReset(@PathVariable Long id) {
        adminUserService.forcePasswordReset(id);
        return ResponseEntity.ok(ApiResponse.ok("Reset email sent"));
    }

    /**
     * Resends the set-password invite email.
     *
     * @param id the user to re-invite
     * @return 200 OK with success message
     */
    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<ApiResponse<Void>> resendInvite(@PathVariable Long id) {
        adminUserService.resendInvite(id);
        return ResponseEntity.ok(ApiResponse.ok("Invite email sent"));
    }
}