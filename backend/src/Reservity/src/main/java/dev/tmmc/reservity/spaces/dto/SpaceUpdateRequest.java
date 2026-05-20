package dev.tmmc.reservity.spaces.dto;

import dev.tmmc.reservity.spaces.entity.SpaceType;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpaceUpdateRequest(
    @Size(max = 120) String name,
    SpaceType type,
    String building,
    UUID buildingId,
    Short floor,
    @Size(max = 20) String room,
    Short seats,
    BigDecimal area,
    BigDecimal price,
    @Size(max = 140) String blurb,
    @Size(max = 2000) String description,
    List<String> amenities,
    List<String> rules,
    List<String> vibes,
    BigDecimal pinX,
    BigDecimal pinY,
    Boolean surprise,
    Boolean dropIn,
    Boolean instantBook,
    SpaceCreateRequest.OperatingHoursDto operatingHours
) {}

