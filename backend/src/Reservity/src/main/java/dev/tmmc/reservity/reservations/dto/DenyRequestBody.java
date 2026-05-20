package dev.tmmc.reservity.reservations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DenyRequestBody(
        @NotBlank @Size(max = 500) String reason
) {}
