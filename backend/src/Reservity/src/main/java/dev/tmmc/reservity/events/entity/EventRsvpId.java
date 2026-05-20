package dev.tmmc.reservity.events.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite PK for {@link EventRsvp}. Must implement {@code Serializable}
 * and {@code equals}/{@code hashCode} for JPA to dedupe in the persistence
 * context.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRsvpId implements Serializable {

    private UUID event;
    private UUID user;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventRsvpId other)) return false;
        return Objects.equals(event, other.event)
                && Objects.equals(user, other.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, user);
    }
}
