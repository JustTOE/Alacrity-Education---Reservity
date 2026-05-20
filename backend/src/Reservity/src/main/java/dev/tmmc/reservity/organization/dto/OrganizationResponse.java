package dev.tmmc.reservity.organization.dto;

import dev.tmmc.reservity.organization.entity.OrgType;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String slug,
        String name,
        String shortName,
        String description,
        String websiteUrl,
        String logoUrl,
        String coverGradient,
        OrgType orgType,
        boolean verified,
        Instant createdAt
) {}
