package com.gastrocontrol.gastrocontrol.dto.auth;

public class RegisterResponse {
    private final Long id;
    private final String email;

    public RegisterResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
}
