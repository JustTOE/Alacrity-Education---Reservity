package dev.tmmc.reservity.spaces.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors the frontend's {@code Space} interface field-for-field.
 * <p>
 * Fields populated in M4 / M5 / M8 (booked, events, viewing, todayBookings,
 * fullyBooked) are returned as their empty/default values for now so the
 * frontend can read them without conditional plumbing.
 */
public record SpaceResponse(
        // The frontend's "id" is our slug — keep so /spaces/:id deep-links work.
        String id,
        UUID uuid,
        String name,
        String type,
        String building,
        UUID buildingId,
        short floor,
        String room,
        short seats,
        BigDecimal area,
        BigDecimal price,
        String currency,
        List<String> vibes,
        List<Integer> booked,
        List<SpaceEventDto> events,
        int viewing,
        int todayBookings,
        Boolean fullyBooked,
        String blurb,
        String description,
        List<String> amenities,
        List<String> rules,
        Pin pin,
        Boolean surprise,
        boolean dropIn,
        boolean instantBook,
        List<SpaceImageResponse> images,
        String primaryImageUrl
) {
    public record Pin(BigDecimal x, BigDecimal y) {}

    public record SpaceEventDto(int startH, int endH, String title, String host) {}
}
