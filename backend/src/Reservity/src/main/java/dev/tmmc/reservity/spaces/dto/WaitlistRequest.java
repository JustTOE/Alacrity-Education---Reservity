package dev.tmmc.reservity.spaces.dto;

import java.time.Instant;

public record WaitlistRequest(
        Instant from,
        Instant to
) {}
