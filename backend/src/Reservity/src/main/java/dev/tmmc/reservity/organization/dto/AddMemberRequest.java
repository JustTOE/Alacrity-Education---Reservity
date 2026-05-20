package dev.tmmc.reservity.organization.dto;

import dev.tmmc.reservity.organization.entity.MembershipRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId,
        MembershipRole role
) {}
