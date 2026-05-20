package dev.tmmc.reservity.spaces.dto;

public record ImageUpdateRequest(
        String altText,
        Short displayOrder,
        Boolean isPrimary
) {}
