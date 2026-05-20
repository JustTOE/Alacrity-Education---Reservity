package dev.tmmc.reservity.reservations.entity;

import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Drop-in / walk-in pass for spaces with {@code drop_in = TRUE}. Schema lives
 * in V6 but no controller until M10. Kept here so the JPA model is complete.
 */
@Entity
@Table(name = "walk_in_passes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkInPass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "pass_token", nullable = false, unique = true, length = 64)
    private String passToken;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (startsAt == null) startsAt = Instant.now();
    }
}
