package dev.tmmc.reservity.messaging.mapper;

import dev.tmmc.reservity.messaging.dto.MessagePreview;
import dev.tmmc.reservity.messaging.dto.MessageResponse;
import dev.tmmc.reservity.messaging.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getDisplayName(),
                m.getContent(),
                m.getEditedAt(),
                m.getCreatedAt()
        );
    }

    public MessagePreview toPreview(Message m) {
        return new MessagePreview(
                m.getId(),
                m.getSender().getId(),
                m.getContent(),
                m.getCreatedAt()
        );
    }
}
