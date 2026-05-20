package dev.tmmc.reservity.reservations.entity;

import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Every booking starts here. Auto-approve flips status to APPROVED at insert
 * time and an associated {@link Reservation} row is created in the same
 * transaction. Non-instant-book spaces leave this PENDING until an owner
 * approves via the M9 UI (or curl in M5).
 *
 * <p>The status state machine is enforced in code (see ReservationStateMachine)
 * and at DB level via the {@code request_decided_when_terminal} CHECK.
 */
@Entity
@Table(name = "reservation_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 20)
    private String reservationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** Generated column. Postgres computes (ends - starts) / 3600. Read-only. */
    @Column(name = "duration_hours", insertable = false, updatable = false, precision = 4, scale = 2)
    private BigDecimal durationHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private ReservationSeries series;

    @Column(name = "series_occurrence_idx")
    private Integer seriesOccurrenceIdx;

    @Column(length = 500)
    private String purpose;

    @Column(name = "attendees_count", nullable = false)
    private short attendeesCount;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(nullable = false, length = 20)
    private ReservationTag tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "auto_approved", nullable = false)
    private boolean autoApproved;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = ReservationStatus.PENDING;
        if (tag == null) tag = ReservationTag.SOLO_FOCUS;
        if (attendeesCount == 0) attendeesCount = 1;
    }
}
