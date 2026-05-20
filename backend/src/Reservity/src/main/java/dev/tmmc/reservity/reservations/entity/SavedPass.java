package dev.tmmc.reservity.reservations.entity;

import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Auto-pinned on every approved reservation so "Recent passes" survives
 * refresh. Composite PK on (user, reservation) prevents duplicates.
 */
@Entity
@Table(name = "saved_passes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPass {

    @EmbeddedId
    private SavedPassId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("reservationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    @PrePersist
    void onCreate() {
        if (savedAt == null) savedAt = Instant.now();
    }
}
