package dev.tmmc.reservity.messaging.repository;

import dev.tmmc.reservity.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    /** Cursor-style: messages in this thread strictly older than {@code before}. */
    Page<Message> findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID conversationId, Instant before, Pageable pageable);

    /**
     * Most-recent message in a thread, used for the conversation-list preview.
     */
    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    /**
     * Unread count for one user in one conversation, when they have read at
     * least once: messages they didn't send that arrived after their
     * {@code last_read_at}.
     */
    @Query("""
            SELECT COUNT(m)
              FROM Message m
             WHERE m.conversation.id = :conversationId
               AND m.sender.id <> :userId
               AND m.createdAt > :lastReadAt
            """)
    long countUnreadSince(@Param("conversationId") UUID conversationId,
                          @Param("userId") UUID userId,
                          @Param("lastReadAt") Instant lastReadAt);

    /** Total unread when the user has never read the thread. */
    @Query("""
            SELECT COUNT(m)
              FROM Message m
             WHERE m.conversation.id = :conversationId
               AND m.sender.id <> :userId
            """)
    long countAllFromOthers(@Param("conversationId") UUID conversationId,
                            @Param("userId") UUID userId);
}
