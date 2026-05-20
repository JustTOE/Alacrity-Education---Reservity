package dev.tmmc.reservity.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSocialDto(
        @NotBlank @Size(max = 24) String kind,
        @NotBlank @Size(max = 120) String handle
) {}
