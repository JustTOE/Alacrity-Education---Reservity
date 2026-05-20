package dev.tmmc.reservity.events.entity;

import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventStatus;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.Space;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A community event. Polymorphic host (USER / ORGANIZATION) mirrors the same
 * pattern used by {@link Space#getOwnerType()}/{@link Space#getOwnerId()}: no
 * DB-level FK on {@code host_id}; resolution happens at the service layer.
 *
 * <p>Location is a fallback chain: {@code space → building → location_label}.
 * All three are nullable so events can be off-platform venues with just a
 * label string ("Auditorium 1, Hawthorn Sciences").
 *
 * <p>{@link #getAttendeesCount()} is denormalized for cheap reads on the
 * landing-page board; it is updated transactionally inside
 * {@code EventRsvpService} so it never drifts from {@code COUNT(GOING)}.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Enumerated(EnumType.STRING)
    @Column(name = "host_type", nullable = false, length = 12)
    private EventHostType hostType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "location_label", length = 160)
    private String locationLabel;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 400)
    private String blurb;

    @Column(length = 4000)
    private String description;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Column(length = 40)
    private String tag;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "attendees_count", nullable = false)
    private int attendeesCount;

    @Column(name = "rsvp_required", nullable = false)
    private boolean rsvpRequired;

    @Column(name = "invite_only", nullable = false)
    private boolean inviteOnly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EventVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EventStatus status;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (visibility == null) visibility = EventVisibility.PUBLIC;
        if (status == null) status = EventStatus.PUBLISHED;
        if (category == null) category = EventCategory.OTHER;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
