package dev.tmmc.reservity.reservations.dto;

import jakarta.validation.constraints.Size;

public record CancelRequestBody(
        @Size(max = 500) String reason   // optional
) {}
