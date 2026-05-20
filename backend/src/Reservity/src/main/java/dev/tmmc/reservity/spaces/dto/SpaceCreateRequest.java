package dev.tmmc.reservity.spaces.dto;

import dev.tmmc.reservity.spaces.entity.SpaceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SpaceCreateRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull SpaceType type,
    String building,
    UUID buildingId,
    short floor,
    @NotBlank @Size(max = 20) String room,
    @PositiveOrZero short seats,
    @NotNull @Positive BigDecimal area,
    @NotNull @PositiveOrZero BigDecimal price,
    @NotBlank @Size(max = 140) String blurb,
    @Size(max = 2000) String description,
    List<String> amenities,
    List<String> rules,
    List<String> vibes,
    BigDecimal pinX,
    BigDecimal pinY,
    boolean surprise,
    boolean dropIn,
    boolean instantBook,
    OperatingHoursDto operatingHours
) {
    public record OperatingHoursDto(
        String mode,
        List<ScheduleEntryDto> schedule
    ) {
        public record ScheduleEntryDto(
            List<String> days,
            int openTime,
            int closeTime
        ) {}
    }
}

