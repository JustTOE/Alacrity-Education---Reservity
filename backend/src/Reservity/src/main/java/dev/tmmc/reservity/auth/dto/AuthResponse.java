package dev.tmmc.reservity.auth.dto;

import dev.tmmc.reservity.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserResponse user
) {}
