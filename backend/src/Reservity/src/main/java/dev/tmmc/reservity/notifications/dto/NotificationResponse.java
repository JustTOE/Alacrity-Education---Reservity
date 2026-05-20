package dev.tmmc.reservity.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String channel,
        String title,
        String body,
        String linkUrl,
        String relatedType,
        UUID relatedId,
        Instant readAt,
        Instant createdAt
) {}
