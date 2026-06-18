package com.example.app.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String fullName,
        Set<String> roles,
        Instant createdAt
) {}
