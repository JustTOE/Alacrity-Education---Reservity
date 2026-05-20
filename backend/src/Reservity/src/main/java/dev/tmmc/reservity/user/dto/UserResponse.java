package dev.tmmc.reservity.user.dto;

import dev.tmmc.reservity.user.entity.AccountType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        boolean emailVerified,
        String handle,
        String displayName,
        String realName,
        String initials,
        AccountType accountType,
        boolean verifiedStudent,
        String bio,
        String avatarUrl,
        String coverGradient,
        LocalDate memberSince,
        boolean twoFaEnabled,
        Instant createdAt
) {}
