package dev.tmmc.reservity.spaces.dto;

import java.util.UUID;

public record SpaceImageResponse(
        UUID id,
        String url,
        String altText,
        Integer width,
        Integer height,
        short displayOrder,
        boolean isPrimary
) {}
