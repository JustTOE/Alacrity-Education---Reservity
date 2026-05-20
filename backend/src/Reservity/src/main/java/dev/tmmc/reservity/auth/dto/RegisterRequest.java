package dev.tmmc.reservity.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 1, max = 80) String displayName,
        @Size(max = 30) @Pattern(regexp = "^[a-z0-9_]{3,30}$",
                message = "Handle must be 3-30 chars: lowercase letters, digits, underscores") String handle
) {}
