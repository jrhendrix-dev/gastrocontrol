package com.gastrocontrol.gastrocontrol.demo;

/**
 * Response DTO for a newly provisioned demo session.
 *
 * @param sessionId the session ID the client must include in the
 *                  {@code X-Demo-Session} header on all subsequent requests
 */
public record DemoSessionResponse(String sessionId) {}