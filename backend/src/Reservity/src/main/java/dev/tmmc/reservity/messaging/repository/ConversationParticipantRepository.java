package dev.tmmc.reservity.messaging.repository;

import dev.tmmc.reservity.messaging.entity.ConversationParticipant;
import dev.tmmc.reservity.messaging.entity.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Modifying
    @Query("""
            UPDATE ConversationParticipant p
               SET p.lastReadAt = :now
             WHERE p.conversation.id = :conversationId
               AND p.user.id = :userId
            """)
    int markRead(@Param("conversationId") UUID conversationId,
                 @Param("userId") UUID userId,
                 @Param("now") Instant now);
}
