package dev.tmmc.reservity.messaging.mapper;

import dev.tmmc.reservity.messaging.dto.ConversationParticipantSummary;
import dev.tmmc.reservity.messaging.dto.ConversationResponse;
import dev.tmmc.reservity.messaging.dto.MessagePreview;
import dev.tmmc.reservity.messaging.entity.Conversation;
import dev.tmmc.reservity.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conv,
                                           User otherParticipant,
                                           MessagePreview lastMessage,
                                           long unreadCount) {
        ConversationParticipantSummary other = otherParticipant == null ? null
                : new ConversationParticipantSummary(
                        otherParticipant.getId(),
                        otherParticipant.getDisplayName(),
                        otherParticipant.getHandle());

        return new ConversationResponse(
                conv.getId(),
                conv.getRequest() == null ? null : conv.getRequest().getId(),
                other,
                lastMessage,
                unreadCount,
                conv.getUpdatedAt(),
                conv.getCreatedAt()
        );
    }
}
