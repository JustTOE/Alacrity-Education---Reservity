package dev.tmmc.reservity.notifications.mapper;

import dev.tmmc.reservity.notifications.dto.NotificationResponse;
import dev.tmmc.reservity.notifications.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getChannel().name(),
                n.getTitle(),
                n.getBody(),
                n.getLinkUrl(),
                n.getRelatedType(),
                n.getRelatedId(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
