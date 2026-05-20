package dev.tmmc.reservity.spaces.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BuildingResponse(
        UUID id,
        String name,
        String shortCode,
        String campusName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Pin pin,
        String heroImageUrl,
        String description
) {
    public record Pin(BigDecimal x, BigDecimal y) {}
}
