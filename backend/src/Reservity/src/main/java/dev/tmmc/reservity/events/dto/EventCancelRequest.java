package dev.tmmc.reservity.events.dto;

import jakarta.validation.constraints.Size;

public record EventCancelRequest(
        @Size(max = 500) String reason
) {}
