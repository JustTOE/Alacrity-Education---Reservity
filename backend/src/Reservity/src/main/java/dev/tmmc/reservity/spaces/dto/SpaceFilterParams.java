package dev.tmmc.reservity.spaces.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Query-string-bound record for {@code GET /api/spaces}. Spring will populate
 * this via {@code @ModelAttribute} (default for non-annotated complex query
 * params); CSV inputs (e.g. {@code vibes=focus,group}) need manual parsing in
 * the controller because Spring doesn't split strings into lists by default
 * for record-bound types.
 */
public record SpaceFilterParams(
        String q,
        String type,
        Short minSeats,
        BigDecimal maxPrice,
        Boolean isFree,
        List<String> vibes,
        UUID buildingId,
        Boolean hideFull
) {}
