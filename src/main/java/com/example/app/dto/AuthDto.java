package com.example.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ── Auth DTOs (Java 21 records) ───────────────────────────────────────────────

public class AuthDto {

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 60) String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            String fullName
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserDto user
    ) {
        public static AuthResponse of(String accessToken, String refreshToken,
                                       long expiresIn, UserDto user) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
        }
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}
}
