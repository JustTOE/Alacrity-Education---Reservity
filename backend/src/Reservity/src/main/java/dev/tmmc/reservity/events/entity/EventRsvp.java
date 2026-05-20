package dev.tmmc.reservity.events.entity;

import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One user's RSVP for one event. Composite PK (event_id, user_id).
 *
 * <p>Status transitions:
 * <ul>
 *   <li>{@code GOING} ↔ {@code CANT_GO}/{@code CANCELLED}: server adjusts
 *       {@link Event#getAttendeesCount()} and may auto-promote the oldest
 *       {@code WAITLIST} row.</li>
 *   <li>{@code GOING} request when at capacity: server downgrades to
 *       {@code WAITLIST}.</li>
 * </ul>
 */
@Entity
@Table(name = "event_rsvps")
@IdClass(EventRsvpId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRsvp {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RsvpStatus status;

    @Column(length = 240)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = RsvpStatus.GOING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
