package dev.tmmc.reservity.messaging.entity;

import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Membership in a {@link Conversation}. Composite PK is (conversation_id, user_id).
 * {@code lastReadAt} drives the unread-message badge — it advances only when the
 * user explicitly hits {@code PATCH /api/conversations/{id}/read}.
 */
@Entity
@Table(name = "conversation_participants")
@IdClass(ConversationParticipantId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationParticipant {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) joinedAt = Instant.now();
    }
}
