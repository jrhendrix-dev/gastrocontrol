package com.gastrocontrol.gastrocontrol.controller.auth;

import com.gastrocontrol.gastrocontrol.dto.auth.LoginRequest;
import com.gastrocontrol.gastrocontrol.dto.auth.LoginResponse;
import com.gastrocontrol.gastrocontrol.dto.auth.RegisterRequest;
import com.gastrocontrol.gastrocontrol.dto.auth.RegisterResponse;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest req) {
        RegisterResponse created = authService.register(req);

        return ResponseEntity.status(201).body(
                ApiResponse.ok("User " + created.getEmail() + " successfully registered", created)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req);
        LoginResponse body = new LoginResponse(token);

        return ResponseEntity.ok(ApiResponse.ok("Login successful", body));
    }
}
