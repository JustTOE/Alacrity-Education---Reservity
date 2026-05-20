package dev.tmmc.reservity.notifications.entity;

import dev.tmmc.reservity.notifications.entity.enums.NotificationChannel;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per user-facing notification. Currently only {@code IN_APP} is
 * persisted in M6 — email sends success/fail in {@code EmailService} and
 * the result is logged, not written here.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private NotificationChannel channel;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 800)
    private String body;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "related_type", length = 24)
    private String relatedType;

    @Column(name = "related_id")
    private UUID relatedId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
