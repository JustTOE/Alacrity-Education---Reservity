package dev.tmmc.reservity.messaging.entity;

import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A messaging thread. Lazy-created on first POST /api/conversations — the
 * 90% auto-approve booking flow that needs no chat creates zero rows.
 *
 * <p>request_id is nullable for forward-compat with free-form 1:1 DMs (not
 * exposed by the M7 controller). Today every conversation pivots on a
 * {@link ReservationRequest}.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private ReservationRequest request;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
