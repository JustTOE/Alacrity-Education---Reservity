package dev.tmmc.reservity.messaging.repository;

import dev.tmmc.reservity.messaging.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Lazy find-or-create lookup. At most one row per request is the application
     * invariant; if a rare race produces two we just pick one.
     */
    Optional<Conversation> findFirstByRequestId(UUID requestId);

    /**
     * "List my conversations", ordered by most-recently-touched first. Joins on
     * the participants table so we don't expose conversations the caller isn't in.
     */
    @Query("""
            SELECT c
              FROM Conversation c
             WHERE EXISTS (
                   SELECT 1 FROM ConversationParticipant p
                    WHERE p.conversation.id = c.id
                      AND p.user.id = :userId)
             ORDER BY c.updatedAt DESC
            """)
    Page<Conversation> findForUser(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Conversation c
               SET c.updatedAt = :now
             WHERE c.id = :id
            """)
    void touch(@Param("id") UUID id, @Param("now") Instant now);
}
