package dev.tmmc.reservity.messaging.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite PK for {@link ConversationParticipant}. Must implement
 * {@code Serializable} and {@code equals}/{@code hashCode} for JPA to
 * dedupe in the persistence context.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipantId implements Serializable {

    private UUID conversation;
    private UUID user;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationParticipantId other)) return false;
        return Objects.equals(conversation, other.conversation)
                && Objects.equals(user, other.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversation, user);
    }
}
