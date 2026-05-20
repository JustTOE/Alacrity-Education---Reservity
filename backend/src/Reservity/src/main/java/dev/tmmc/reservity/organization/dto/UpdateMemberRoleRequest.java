package dev.tmmc.reservity.organization.dto;

import dev.tmmc.reservity.organization.entity.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull MembershipRole role) {}
