package dev.tmmc.reservity.reservations.entity;

import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The blocking row. Created only when a {@link ReservationRequest} reaches
 * APPROVED. The DB enforces no-overlap-per-space via a GiST exclusion
 * constraint (see V6 migration), so a competing request that races with an
 * approval will trigger {@code DataIntegrityViolationException} on insert.
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private ReservationRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "checkin_at")
    private Instant checkinAt;

    @Column(name = "checkout_at")
    private Instant checkoutAt;

    @Column(name = "no_show", nullable = false)
    private boolean noShow;

    /** 64-char hex (256 random bits). The QR payload. */
    @Column(name = "pass_token", nullable = false, unique = true, length = 64)
    private String passToken;

    @Column(name = "pass_revoked", nullable = false)
    private boolean passRevoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
