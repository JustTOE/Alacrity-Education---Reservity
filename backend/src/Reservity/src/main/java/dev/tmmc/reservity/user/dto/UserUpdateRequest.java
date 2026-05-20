package dev.tmmc.reservity.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 80) String displayName,
        @Size(max = 120) String realName,
        @Size(max = 4) String initials,
        @Size(max = 500) String bio,
        @Size(max = 120) String coverGradient,
        @Pattern(regexp = "^[a-z0-9_]{3,30}$",
                message = "Handle must be 3-30 chars: lowercase letters, digits, underscores")
        String handle
) {}
