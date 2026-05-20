package dev.tmmc.reservity.organization.dto;

import dev.tmmc.reservity.organization.entity.OrgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizationCreateRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "^[a-z0-9-]{2,60}$") String slug,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 40) String shortName,
        @Size(max = 2000) String description,
        @Size(max = 500) String websiteUrl,
        @Size(max = 120) String coverGradient,
        OrgType orgType
) {}
