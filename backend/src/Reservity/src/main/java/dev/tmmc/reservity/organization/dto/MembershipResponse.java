package dev.tmmc.reservity.organization.dto;

import dev.tmmc.reservity.organization.entity.MembershipRole;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
        UUID userId,
        String userHandle,
        String userDisplayName,
        UUID orgId,
        MembershipRole role,
        Instant joinedAt
) {}
