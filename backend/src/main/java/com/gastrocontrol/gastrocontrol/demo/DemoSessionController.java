package com.gastrocontrol.gastrocontrol.demo;

import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for demo session lifecycle management.
 *
 * <p>Exposes a single endpoint that Angular calls on first load to
 * provision an isolated demo schema for the visiting user.
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoSessionController {

    private final DemoSessionService demoSessionService;

    /**
     * Provisions a new demo session schema and returns the session ID.
     *
     * <p>Angular stores this ID in {@code sessionStorage} and sends it
     * back on every subsequent request via the {@code X-Demo-Session} header.
     *
     * @return session ID to be used as the tenant identifier
     *
     * <p><b>Postman:</b> {@code POST /api/demo/session} — no body, no auth required.
     * Response: {@code { "data": { "sessionId": "abc123def456" } }}
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<DemoSessionResponse>> createSession() {
        log.info("New demo session requested");
        String sessionId = demoSessionService.provisionSession();
        return ResponseEntity.ok(ApiResponse.ok("Demo session created", new DemoSessionResponse(sessionId)));
    }
}